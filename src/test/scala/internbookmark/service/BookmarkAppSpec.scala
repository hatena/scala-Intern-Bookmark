package internbookmark.service

import internbookmark.helper._
import internbookmark.helper.Factory._
import internbookmark.repository
import org.joda.time.LocalDateTime

import scala.util.Random

class BookmarkAppSpec extends UnitSpec with SetupDB {
  private def createApp(): BookmarkApp = new BookmarkApp(Random.nextInt().toString) {
    override val entriesRepository = new repository.Entries {
      override def titleExtractor: repository.TitleExtractor = new repository.TitleExtractor {
        def fetch(url: String): Option[String] = Some("<title>test-title</title>")
      }
    }
  }

  describe("BookmarkApp") {
    it("should save data when new bookmark without a comment is added") {
      val app = createApp()
      app.add("http://www.example.com", "").fold({ _ => fail() }, { bookmark =>
        bookmark.comment shouldBe 'empty
        bookmark.entry.url shouldBe "http://www.example.com"
      })
      val bookmarks = app.list()
      bookmarks.length shouldBe 1
      bookmarks.head.comment shouldBe 'empty
      bookmarks.head.entry.url shouldBe "http://www.example.com"
    }

    it("should save data when new bookmark with a comment is added") {
      val app = createApp()
      app.add("http://www.example.com", "very cool website!!").fold({ _ => fail() }, { bookmark =>
        bookmark.comment shouldBe "very cool website!!"
        bookmark.entry.url shouldBe "http://www.example.com"
      })
      val bookmarks = app.list()
      bookmarks.length shouldBe 1
      bookmarks.head.comment shouldBe "very cool website!!"
      bookmarks.head.entry.url shouldBe "http://www.example.com"
    }

    it("should overwrite a comment of the saved bookmark") {
      val app = createApp()
      app.add("http://www.example.com", "very cool website!!")
      app.add("http://www.example.com", "too bad...")

      val bookmarks = app.list()
      bookmarks.length shouldBe 1
      bookmarks.head.comment shouldBe "too bad..."
      bookmarks.head.entry.url shouldBe "http://www.example.com"
    }

    it("should list all saved bookmarks") {
      val app = createApp()
      app.add("http://www.example.com", "very cool website!!")
      app.add("http://www.example.org", "best website ever!!")
      val bookmarks = app.list()
      bookmarks.length shouldBe 2
      bookmarks.map( b => (b.entry.url, b.comment)) should contain theSameElementsAs List(
        ("http://www.example.com", "very cool website!!"),
        ("http://www.example.org", "best website ever!!")
      )
    }

    it("should delete a bookmark corresponds to a given url") {
      val app = createApp()
      app.add("http://www.example.com", "very cool website!!")
      app.deleteByUrl("http://www.example.com")
      val bookmarks = app.list()
      bookmarks.length shouldBe 0
    }

    it("should return error when an app delete a non-existent bookmark") {
      val app = createApp()
      app.add("http://www.example.com", "very cool website!!")
      app.deleteByUrl("http://www.example.com") shouldBe Right(())
      app.deleteByUrl("http://www.example.com") shouldBe Left(BookmarkNotFoundError)
    }

    it("should list saved bookmarks at a specified page") {
      val app = createApp()
      val b1 = createBookmark(Some(app.currentUser), updatedAt = new LocalDateTime(2015, 4, 10, 0, 0, 0))
      val b2 = createBookmark(Some(app.currentUser), updatedAt = new LocalDateTime(2015, 4, 11, 0, 0, 0))
      val b3 = createBookmark(Some(app.currentUser), updatedAt = new LocalDateTime(2015, 4, 12, 0, 0, 0))

      val bookmarksPage1 = app.listPaged(1, 2)
      bookmarksPage1.length shouldBe 2
      bookmarksPage1.map( _.id ) shouldBe List(b3.id, b2.id)

      val bookmarksPage2 = app.listPaged(2, 2)
      bookmarksPage2.length shouldBe 1
      bookmarksPage2.map( _.id ) shouldBe List(b1.id)
    }
  }
}
