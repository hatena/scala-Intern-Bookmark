package jp.ne.hatena.intern.scalatra
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import com.ning.http.client.oauth._
import dispatch.{Http => _, Future => _, _}
import dispatch.oauth._
import org.slf4j.LoggerFactory
import scala.concurrent._

import org.scalatra._
import org.scalatra.auth._

import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.{Success, Failure}

case class HatenaOAuthUserName(id: String)

class HatenaOAuth extends ScalatraServlet with HatenaOAuthSupport {
  get("/login") {
    if (isAuthenticated) {
      // nop
    } else {
      scentry.authenticate()
    }
    Found("/")
  }
}

trait HatenaOAuthSupport extends ScentrySupport[HatenaOAuthUserName] {
  self: ScalatraBase =>

  override protected def fromSession: PartialFunction[String, HatenaOAuthUserName] = {
    case id: String => HatenaOAuthUserName(id)
  }

  override protected def toSession: PartialFunction[HatenaOAuthUserName, String] = {
    case HatenaOAuthUserName(id) => id
  }

  override protected def scentryConfig: ScentryConfiguration = new ScentryConfig {}.asInstanceOf[ScentryConfiguration]

  override protected def configureScentry() = {
    scentry.unauthenticated {
      scentry.strategies("HatenaOAuth").unauthenticated()
    }
  }

  override protected def registerAuthStrategies() = {
    scentry.register("HatenaOAuth", app => new HatenaOAuthStrategy(app))
  }
}

trait HatenaHttp extends SomeHttp {
  def http: HttpExecutor = dispatch.Http
}

trait HatenaConsumer extends SomeConsumer {
  val consumerKey = Option(System.getProperty("hatenaoauth.consumerkey")).getOrElse(throw new IllegalStateException("No consumerkey is set"))
  val consumerSecret = Option(System.getProperty("hatenaoauth.consumersecret")).getOrElse(throw new IllegalStateException("No consumersecrete is set"))

  def consumer: ConsumerKey = new ConsumerKey(consumerKey, consumerSecret)
}

trait HatenaCallback extends SomeCallback {
  def urlBase: String
  def callback: String = s"$urlBase/login"
}

trait HatenaEndpoints extends SomeEndpoints {
  def requestToken: String = "https://www.hatena.com/oauth/initiate"
  def accessToken: String = "https://www.hatena.com/oauth/token"
  def authorize: String = "https://www.hatena.com/oauth/authorize"
}

class HatenaExchange(val urlBase: String) extends Exchange
  with HatenaHttp with HatenaConsumer with HatenaCallback with HatenaEndpoints {

  override def fetchRequestToken(implicit executor: ExecutionContext): Future[Either[String,RequestToken]] = {
    val promised = http(
      url(requestToken)
        << Map("oauth_callback" -> callback, "scope" -> "read_public")
        <@ consumer
        > as.oauth.Token
    )
    for (eth <- message(promised, "request token")) yield eth.joinRight
  }
}

class HatenaOAuthStrategy(protected val app: ScalatraBase) extends ScentryStrategy[HatenaOAuthUserName] {
  import scala.concurrent.ExecutionContext.Implicits.global

  val logger = LoggerFactory.getLogger("hatenaoauth")

  private[this] val REQUEST_TOKEN_SESSION_KEY ="hatena_request_token"

  private[this] def fetchUserInfo(exchange: HatenaExchange, accessToken: RequestToken): Future[String] = {
    val u = url("http://n.hatena.com/applications/my.json") <@(exchange.consumer, accessToken)
    dispatch.Http(u.GET).flatMap { res =>
      import org.json4s._
      import org.json4s.jackson.JsonMethods._
      logger.debug(res.getResponseBody)
      val json = parse(res.getResponseBody)
      ((for {
        JObject(ob) <- json
        JField("url_name", JString(urlName)) <- ob
      } yield urlName): List[String]).headOption match {
        case Some(urlName) =>
          Future.successful(urlName)
        case None =>
          Future.failed(new IllegalStateException("Requesting to API is failed."))
      }
    }
  }

  override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[HatenaOAuthUserName] = {
    val exchange = new HatenaExchange(app.fullUrl("/"))
    app.params.get("oauth_verifier") flatMap { verifier =>
      val requestToken = app.session.getAttribute(REQUEST_TOKEN_SESSION_KEY).asInstanceOf[RequestToken]
      val req = for {
        accessToken <- exchange.fetchAccessToken(requestToken, verifier).flatMap {
          case Right(accessToken) =>
            app.session.removeAttribute(REQUEST_TOKEN_SESSION_KEY)
            Future.successful(accessToken)
          case Left(msg) =>
            logger.debug(msg)
            Future.failed( new IllegalStateException(msg) )
        }
        userInfo <- fetchUserInfo(exchange, accessToken)
      } yield HatenaOAuthUserName(userInfo)
      Await.ready(req, Duration.Inf)
      req.value.flatMap(_.toOption)
    }
  }

  override def unauthenticated()(implicit request: HttpServletRequest, response: HttpServletResponse) {
    val exchange = new HatenaExchange(app.fullUrl("/"))
    logger.debug(exchange.urlBase)
    val req = exchange.fetchRequestToken.flatMap {
      case Right(requestToken) =>
        app.session.setAttribute(REQUEST_TOKEN_SESSION_KEY, requestToken)
        Future.successful( exchange.signedAuthorize(requestToken) )
      case Left(msg) =>
        logger.debug(msg)
        Future.failed( new IllegalStateException(msg) )
    }
    Await.ready(req, Duration.Inf)
    req.value.foreach {
      case Success(url) =>
        app.redirect(url)
      case Failure(t) =>
        logger.debug(t.toString)
        app.halt(InternalServerError("Preparing authentication failed"))
    }
  }
}
