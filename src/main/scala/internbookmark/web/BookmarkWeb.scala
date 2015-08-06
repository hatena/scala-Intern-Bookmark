package internbookmark.web

import jp.ne.hatena.intern.scalatra.HatenaOAuthSupport

import scala.util.control.Exception

import internbookmark.service.BookmarkApp
import javax.servlet.http.HttpServletRequest

import org.scalatra._
import org.slf4j.LoggerFactory

class BookmarkWeb extends BookmarkWebStack with BookmarkAPIWeb with HatenaOAuthSupport with AppContextSupport {
  val logger = LoggerFactory.getLogger("bookmarkweb")

  before() {
    if (isAuthenticated) {
      // nop
    } else {
      halt(Found("/auth/login"))
    }
  }

  def currentUserName()(implicit request: HttpServletRequest): String = {
    scentry.user.id
  }

  def createApp()(implicit request: HttpServletRequest): BookmarkApp =
    new BookmarkApp(currentUserName())

  get("/") {
    Found("/bookmarks")
  }

  get("/bookmarks") {
    val app = createApp()
    internbookmark.html.list(app.list())
  }

  get("/bookmarks/add") {
    internbookmark.html.add()
  }

  post("/bookmarks/add") {
    val app = createApp()
    (for {
      url <- params.get("url").toRight(BadRequest()).right
      bookmark <- app.add(url, params.getOrElse("comment", "")).left.map(_ => InternalServerError()).right
    } yield bookmark) match {
      case Right(bookmark) =>
        Found(s"/bookmarks")
      case Left(errorResult) => errorResult
    }
  }

  get("/bookmarks/:id/edit") {
    val app = createApp()
    (for {
      rawId <- params.get("id").toRight(BadRequest()).right
      id <- Exception.catching(classOf[NumberFormatException]).either(rawId.toLong).left.map(_ => BadRequest()).right
      bookmark <- app.find(id).toRight(NotFound()).right
    } yield bookmark) match {
      case Right(bookmark) =>
        internbookmark.html.edit(bookmark)
      case Left(errorResult) => errorResult
    }
  }

  post("/bookmarks/:id/edit") {
    val app = createApp()
    (for {
      rawId <- params.get("id").toRight(BadRequest()).right
      id <- Exception.catching(classOf[NumberFormatException]).either(rawId.toLong).left.map(_ => BadRequest()).right
      _ <- Right(logger.debug(id.toString)).right
      bookmark <- app.edit(id, params.getOrElse("comment", "")).left.map(_ => NotFound()).right
    } yield bookmark) match {
      case Right(bookmark) =>
        Found(s"/bookmarks/${bookmark.id}/edit")
      case Left(errorResult) => errorResult
    }
  }

  get("/bookmarks/:id/delete") {
    val app = createApp()
    (for {
      rawId <- params.get("id").toRight(BadRequest()).right
      id <- Exception.catching(classOf[NumberFormatException]).either(rawId.toLong).left.map(_ => BadRequest()).right
      bookmark <- app.find(id).toRight(NotFound()).right
    } yield bookmark) match {
      case Right(bookmark) =>
        internbookmark.html.delete(bookmark)
      case Left(errorResult) => errorResult
    }
  }

  post("/bookmarks/:id/delete") {
    val app = createApp()
    (for {
      rawId <- params.get("id").toRight[ActionResult](BadRequest()).right
      id <- Exception.catching(classOf[NumberFormatException]).either(rawId.toLong).left.map(_ => BadRequest()).right
      _ <- app.delete(id).left.map(_ => NotFound()).right
    } yield ()) match {
      case Right(()) =>
        Found(s"/bookmarks")
      case Left(errorResult) => errorResult
    }
  }
}

