package internbookmark.web

import org.json4s.Formats
import org.json4s.JsonAST.{JString, JObject}
import org.scalatra._
import org.scalatra.json.JacksonJsonSupport
import scala.util.control.Exception

trait BookmarkAPIWeb extends JacksonJsonSupport { self: BookmarkWeb with AppContextSupport =>
  protected implicit val jsonFormats: Formats =
    internbookmark.service.Json.Formats

  val PAGE_LIMIT = 10

  get("/api/bookmarks") {
    contentType = formats("json")
    val app = createApp()

    val page = (for {
      rawPage <- params.get("page")
      page <- Exception.catching(classOf[NumberFormatException]).
        opt(rawPage.toInt).filter(_ > 0)
    } yield page).getOrElse(1)

    app.listPaged(page, PAGE_LIMIT)
  }

  post("/api/bookmark") {
    contentType = formats("json")
    val json = parsedBody

    val app = createApp()
    val req = for {
      url <- (json \ "url").extractOpt[String].toRight(BadRequest()).right
      comment <- Right((json \ "comment").extractOrElse("")).right
      bookmark <- app.add(url, comment).left.map(_ => InternalServerError()).right
    } yield bookmark
    req match {
      case Right(bookmark) => bookmark
      case Left(errorResult) => errorResult.copy(
        body = JObject("error" -> JString(errorResult.status.message)))
    }
  }
}
