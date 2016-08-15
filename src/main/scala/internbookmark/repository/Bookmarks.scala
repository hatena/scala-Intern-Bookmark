package internbookmark.repository

import internbookmark.repository
import internbookmark.model.{Entry, Bookmark, User}
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.LocalDateTime
import slick.driver.MySQLDriver.api._
import slick.jdbc.GetResult
import com.github.tototoshi.slick.MySQLJodaSupport._

object Bookmarks {

  private case class BookmarkRow(id: Long, userId: Long, entryId: Long, comment: String, createdAt: LocalDateTime, updatedAt: LocalDateTime) {
    def toBookmark(user: User, entry: Entry): Bookmark =
      Bookmark(id, user, entry, comment, createdAt, updatedAt)
  }

  private implicit val getBookmarkRowResult = GetResult(r => BookmarkRow(r.<<, r.<<, r.<<, r.<<, r.<<, r.<<))

  def createOrUpdate(user: User, entry: Entry, comment: String)(implicit ctx: Context): Unit =
   findByEntry(user, entry) match {
     case Some(bookmarkRow) =>
       val updatedAt = new LocalDateTime()
       run(sqlu"""
         UPDATE bookmark
           SET
             comment = $comment,
             updated_at = $updatedAt
           WHERE
             id = ${bookmarkRow.id}
       """.map(_ => ()))
     case None => {
       val id = Identifier.generate
       val bookmark: Bookmark = Bookmark(id, user, entry, comment, new LocalDateTime(), new LocalDateTime())
       run(sqlu"""
         INSERT INTO bookmark
           (id, user_id, entry_id, comment, created_at, updated_at)
           VALUES
           (
             ${bookmark.id},
             ${bookmark.user.id},
             ${bookmark.entry.id},
             ${bookmark.comment},
             ${bookmark.createdAt},
             ${bookmark.updatedAt}
           )
       """.map(_ => ()))
     }
   }

  def delete(bookmark: Bookmark)(implicit ctx: Context): Unit = {
    val _: Int = run(sqlu"""DELETE FROM bookmark WHERE id = ${bookmark.id} """)
  }

  def findByEntry(user: User, entry: Entry)(implicit ctx: Context): Option[Bookmark] = run(
    sql"""
      SELECT * FROM bookmark
        WHERE user_id = ${user.id} AND entry_id = ${entry.id} LIMIT 1
    """.as[BookmarkRow].map(_.headOption.map(_.toBookmark(user, entry)))
  )

  def find(bookmarkId: Long)(implicit ctx: Context): Option[Bookmark] = for {
    bookmarkRow <- run(sql"""
      SELECT * FROM bookmark
        WHERE id = $bookmarkId LIMIT 1
    """.as[BookmarkRow].map(_.headOption))
    entry <- repository.Entries.find(bookmarkRow.entryId)
    user <- repository.Users.find(bookmarkRow.userId)
  } yield bookmarkRow.toBookmark(user, entry)

  private def loadBookmarks(user: User, bookmarkRows: Seq[BookmarkRow])(implicit ctx: Context): Seq[Bookmark] = {
    val entries = repository.Entries.searchByIds(bookmarkRows.map(_.entryId))
    val entryById = entries.map(e => e.id -> e).toMap
    bookmarkRows.map(row => entryById.get(row.entryId).map(row.toBookmark(user, _))).flatten
  }

  def listAll(user: User)(implicit ctx: Context): Seq[Bookmark] = {
    val rows = run(sql"""
        SELECT * FROM bookmark
          WHERE user_id = ${user.id} ORDER BY updated_at DESC
      """.as[BookmarkRow])
    loadBookmarks(user, rows)
  }

  def listPaged(user: User, page: Int, limit: Int)(implicit ctx: Context): Seq[Bookmark] = {
    require( page > 0 )
    require( limit > 0 )

    val offset = (page - 1) * limit

    val rows = run(sql"""
      SELECT * FROM bookmark
        WHERE user_id = ${user.id} ORDER BY updated_at DESC LIMIT $offset,$limit
    """.as[BookmarkRow])
    loadBookmarks(user, rows)
  }
}
