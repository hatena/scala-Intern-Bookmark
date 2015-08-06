package internbookmark.web

import javax.servlet.http.HttpServletRequest

import internbookmark.service.BookmarkApp
import internbookmark.repository
import internbookmark.model._
import internbookmark.helper.Factory._
import internbookmark.helper.{EntriesForTest, SetupDB, WebUnitSpec}

import scala.util.Random

class BookmarkWebForTest extends BookmarkWeb {
  override def createApp()(implicit request: HttpServletRequest ): BookmarkApp = new BookmarkApp(currentUserName()) {
    override val entriesRepository = EntriesForTest
  }

  override def isAuthenticated(implicit request: HttpServletRequest): Boolean =
    request.cookies.get("USER").isDefined

  override def currentUserName()(implicit request: HttpServletRequest): String = {
    request.cookies.getOrElse("USER", throw new IllegalStateException())
  }
}

class BookmarkWebSpec extends WebUnitSpec with SetupDB {

  addServlet(classOf[BookmarkWebForTest], "/*")

  val testUserName = Random.nextInt().toString
  def testUser()(implicit ctx: repository.Context): User =
    repository.Users.findOrCreateByName(testUserName)
  def withUserSessionHeader(headers: Map[String, String] = Map.empty) = {
    headers + ("Cookie" -> s"USER=$testUserName;")
  }

  describe("BookmarkWeb") {
    it("should redirect to login page for an unauthenticated access") {
      get("/bookmarks") {
        status shouldBe 302
        header.get("Location") should contain("/auth/login")
      }
    }

    it("should redirect to the list page when the top page is accessed") {
      get("/",
        headers = withUserSessionHeader()
      ) {
        status shouldBe 302
        header.get("Location") should contain ("/bookmarks")
      }
    }

    it("should show list of bookmarks") {
      get("/bookmarks",
        headers = withUserSessionHeader()
      ) {
        status shouldBe 200
      }
    }

    it("should show a page to add a bookmark") {
      get("/bookmarks/add",
        headers = withUserSessionHeader()
      ) {
        status shouldBe 200
      }
    }

    it("should make new bookmark when the bookmark with an empty comment is posted") {
      val url = s"http://www.example.com?${ Random.nextInt() }"

      post("/bookmarks/add",
        params = List("url" -> url, "comment" -> ""),
        headers = withUserSessionHeader()
      ) {
        status shouldBe 302
        header.get("Location") should contain("/bookmarks")

        val maybeEntry = repository.Entries.findByUrl(url)
        maybeEntry shouldBe 'defined

        val maybeBookmark = repository.Bookmarks.findByEntry(testUser(), maybeEntry.get)
        maybeBookmark shouldBe 'defined
        maybeBookmark.get.comment shouldBe 'empty
      }
    }

    it("should make new bookmark when the bookmark with an non-empty comment is posted") {
      val url = s"http://www.example.com?${ Random.nextInt() }"

      post("/bookmarks/add",
        params = List("url" -> url, "comment" -> "hello~"),
        headers = withUserSessionHeader()
      ) {
        status shouldBe 302
        header.get("Location") should contain("/bookmarks")

        val maybeEntry = repository.Entries.findByUrl(url)
        maybeEntry shouldBe 'defined

        val maybeBookmark = repository.Bookmarks.findByEntry(testUser(), maybeEntry.get)
        maybeBookmark shouldBe 'defined
        maybeBookmark.get.comment shouldBe "hello~"
      }
    }

    it("should show a page to edit a bookmark") {
      val url = s"http://www.example.com?${Random.nextInt()}"
      val entry = createEntry(url = url)
      val bookmark = createBookmark(Some(testUser()), Some(entry), comment = "not changed")

      get(s"/bookmarks/${bookmark.id}/edit",
        headers = withUserSessionHeader()
      ) {
        status shouldBe 200
      }
    }

    it("should change a bookmark specified by the url") {
      val url = s"http://www.example.com?${Random.nextInt()}"
      val entry = createEntry(url = url)
      val bookmark = createBookmark(Some(testUser()), Some(entry), comment = "not changed")

      post(s"/bookmarks/${bookmark.id}/edit",
        params = List("url" -> url, "comment" -> "changed!!"),
        headers = withUserSessionHeader()
      ) {
        status shouldBe 302
        header.get("Location") should contain(s"/bookmarks/${ bookmark.id }/edit")

        val maybeBookmark = repository.Bookmarks.findByEntry(testUser(), entry)
        maybeBookmark shouldBe 'defined
        maybeBookmark.get.comment shouldBe "changed!!"
      }
    }

    it("should show a page to delete a bookmark") {
      val url = s"http://www.example.com?${Random.nextInt()}"
      val entry = createEntry(url = url)
      val bookmark = createBookmark(Some(testUser()), Some(entry), comment = "not changed")

      get(s"/bookmarks/${bookmark.id}/delete",
        headers = withUserSessionHeader()
      ) {
        status shouldBe 200
      }
    }

    it("should delete a bookmark specified by the url") {
      val url = s"http://www.example.com?${Random.nextInt()}"
      val entry = createEntry(url = url)
      val bookmark = createBookmark(Some(testUser()), Some(entry), comment = "not changed")

      post(s"/bookmarks/${bookmark.id}/delete",
        params = List(),
        headers = withUserSessionHeader()
      ) {
        status shouldBe 302
        header.get("Location") should contain(s"/bookmarks")

        val maybeBookmark = repository.Bookmarks.findByEntry(testUser(), entry)
        maybeBookmark shouldBe 'empty
      }
    }

    it("should return a json representation of list of bookmarks") {
      (1 to 10).foreach(_ => createBookmark(Some(testUser())))

      implicit val formats = internbookmark.service.Json.Formats
      import org.json4s.jackson.Serialization.{ read => jsonRead }
      get("/api/bookmarks",
        params = List(),
        headers = withUserSessionHeader()
      ) {
        status shouldBe 200
        inside(header.get("Content-Type")) {
          case Some(contentType) =>
            contentType should startWith ("application/json")
        }

        val bookmarks = jsonRead[List[Bookmark]](body)
        bookmarks.size shouldBe 10
      }
    }

    it("should add a new bookmark and return its json representaion") {
      implicit val formats = internbookmark.service.Json.Formats
      import org.json4s.jackson.Serialization.{ read => jsonRead }
      import org.json4s._
      import org.json4s.jackson.JsonMethods

      val url = s"http://www.example.com?${ Random.nextInt() }"

      post("/api/bookmark",
        body = JsonMethods.compact(
          JsonMethods.render(
            JObject( "url" -> JString(url), "comment" -> JString("hello!"))
          )
        ),
        headers = withUserSessionHeader()
      ) {
        status shouldBe 200
        inside(header.get("Content-Type")) {
          case Some(contentType) =>
            contentType should startWith ("application/json")
        }

        val bookmark = jsonRead[Bookmark](body)
        bookmark.entry.url shouldBe url
        bookmark.comment shouldBe "hello!"
      }
    }

    it("should respond 400 with invalid id") {
      val reqs = Seq[(=> Unit) => Unit](
        get[Unit]("/bookmarks/hoge/edit",
          headers = withUserSessionHeader()
        ),
        get[Unit]("/bookmarks/hoge/delete",
          headers = withUserSessionHeader()
        ),
        post[Unit]("/bookmarks/hoge/edit",
          params = List(),
          headers = withUserSessionHeader()
        ),
        post[Unit]("/bookmarks/hoge/delete",
          params = List(),
          headers = withUserSessionHeader()
        )
      )
      reqs.foreach { req =>
        req {
          status shouldBe 400
        }
      }
    }

    it("should respond 404 with non-existing id") {
      val bookmarkId = {
        val bookmark = createBookmark(Some(testUser()))
        val id = bookmark.id
        repository.Bookmarks.delete(bookmark)
        id
      }

      val reqs = Seq[(=> Unit) => Unit](
        get(s"/bookmarks/$bookmarkId/edit",
          headers = withUserSessionHeader()
        ),
        get(s"/bookmarks/$bookmarkId/delete",
          headers = withUserSessionHeader()
        ),
        post(s"/bookmarks/$bookmarkId/edit",
          params = List(),
          headers = withUserSessionHeader()
        ),
        post(s"/bookmarks/$bookmarkId/delete",
          params = List(),
          headers = withUserSessionHeader()
        )
      )
      reqs.foreach { req =>
        req {
          status shouldBe 404
        }
      }
    }
  }
}
