package internbookmark.helper

import org.joda.time.LocalDateTime
import internbookmark.model._
import internbookmark.repository
import internbookmark.repository.{Context, TitleExtractor}

import scala.util.Random

object Factory {
  def createUser(
    name: String = Random.nextInt().toString
  )(implicit ctx: Context): User =
    repository.Users.createByName(name)

  def createEntry(
    url: String = "http://example.com?" + Random.nextInt().toString,
    title: String = Random.nextInt().toString
  )(implicit ctx: Context): Entry =
    new repository.Entries {
      val titleExtractor = new TitleExtractor {
        override protected def fetch(url: String) = None
        override def extract(url: String) = Some(title)
      }
    }.createByUrl(url)

  def createBookmark(
    optionalUser: Option[User] = None,
    optionalEntry: Option[Entry] = None,
    comment: String = "",
    updatedAt: LocalDateTime = new LocalDateTime()
  )(implicit ctx: Context): Bookmark = {
    import slick.driver.MySQLDriver.api._
    import com.github.tototoshi.slick.MySQLJodaSupport._

    val user = optionalUser.getOrElse(createUser())
    val entry = optionalEntry.getOrElse(createEntry())
    repository.Bookmarks.createOrUpdate(user, entry, comment)
    val bookmark = repository.Bookmarks.findByEntry(user, entry).get
    Context.runDBIO(sqlu"""UPDATE bookmark SET updated_at = ${updatedAt} where id = ${bookmark.id}""")
    repository.Bookmarks.findByEntry(user, entry).get
  }
}
