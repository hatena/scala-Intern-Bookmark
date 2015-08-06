package internbookmark.cli

import internbookmark.service.{BookmarkApp, Context}
import internbookmark.model.Bookmark

import scala.sys.process

object BookmarkCLI {
  def main(args: Array[String]): Unit = {
    val exitStatus = run(args)
    sys.exit(exitStatus)
  }

  def createApp(userName: String): BookmarkApp = new BookmarkApp(userName)

  def run(args: Array[String]): Int = {
    sys.env.get("USER") match {
      case Some(userName) =>
        try {
          Context.setup("db.default")
          implicit val ctx = Context.createContext()
          val app = createApp(userName)
          args.toList match {
            case "add" :: url :: rest =>
              val comment = rest.headOption.getOrElse("")
              add(app, url, comment)
            case "delete" :: url :: _ =>
              delete(app, url)
            case "list" :: _ =>
              list(app)
            case _ =>
              help()
          }
        } finally Context.destroy
      case None =>
        process.stderr.println("USER environment must be set.")
        1
    }
  }

  def add(app: BookmarkApp, url: String, comment: String)(implicit ctx: Context): Int = {
    app.add(url, comment) match {
      case Right(bookmark) =>
        println("Bookmarked " + prettifyBookmark(bookmark))
        0
      case Left(error) =>
        process.stderr.println(error.toString())
        1
    }
  }

  def delete(app: BookmarkApp, url: String)(implicit ctx: Context): Int = {
    app.deleteByUrl(url) match {
      case Right(_) =>
        println(s"Deleted $url.")
        0
      case Left(error) =>
        process.stderr.println(error.toString())
        1
    }
  }

  def list(app: BookmarkApp)(implicit ctx: Context): Int = {
    println(s"--- ${app.currentUser.name}'s Bookmarks ---")
    app.list().foreach { bookmark =>
      println(prettifyBookmark(bookmark))
    }
    0
  }

  def help(): Int = {
    process.stderr.println(
      """
        | usage:
        |   run add url [comment]
        |   run list
        |   run delete url
      """.stripMargin)
    1
  }

  private[this] def prettifyBookmark(bookmark: Bookmark): String =
    bookmark.entry.title + " (" + bookmark.entry.url + ") " + (if (bookmark.comment == "") "" else  " " + bookmark.comment)
}
