package internbookmark.repository

import internbookmark.model.Entry
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.LocalDateTime
import com.github.tarao.slickjdbc.interpolation.SQLInterpolation._
import com.github.tarao.slickjdbc.interpolation.CompoundParameter._
import slick.jdbc.GetResult
import com.github.tototoshi.slick.MySQLJodaSupport._
import com.github.tarao.slickjdbc.util.NonEmpty

trait Entries {
  def titleExtractor: TitleExtractor

  private type EntryRow = Entry
  private val EntryRow = Entry
  private implicit val getUserRowResult = GetResult(r => EntryRow(r.<<, r.<<, r.<<, r.<<, r.<<))

  def find(entryId: Long)(implicit ctx: Context): Option[Entry] = run(
    sql"SELECT * FROM entry WHERE id = ${entryId} LIMIT 1".as[EntryRow].map(_.headOption)
  )

  def findByUrl(url: String)(implicit ctx: Context): Option[Entry] = run(
    sql"SELECT * FROM entry WHERE url = ${url} LIMIT 1".as[EntryRow].map(_.headOption)
  )

  def findOrCreateByUrl(url: String)(implicit ctx: Context): Entry =
    findByUrl(url).getOrElse(createByUrl(url))

  def createByUrl(url: String)(implicit ctx: Context): Entry = {
    val id = Identifier.generate
    val entry = Entry(id, url, titleExtractor.extract(url).getOrElse(""), new LocalDateTime(), new LocalDateTime())
    run(sqlu"""
      INSERT INTO entry
        (id, url, title, created_at, updated_at)
        VALUES
        (
          ${entry.id},
          ${entry.url},
          ${entry.title},
          ${entry.createdAt},
          ${entry.updatedAt}
        )
    """)
    entry
  }

  def searchByIds(ids: Seq[Long])(implicit ctx: Context): Seq[Entry] =
    NonEmpty.fromTraversable(ids).fold(Seq(): Seq[EntryRow]){ nel =>
      run(sql"SELECT * FROM entry WHERE id IN $nel".as[EntryRow])
    }
}

object Entries extends Entries {
  val titleExtractor = TitleExtractorDispatch
}
