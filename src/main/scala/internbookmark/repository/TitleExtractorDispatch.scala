package internbookmark.repository

import dispatch._, Defaults._

object TitleExtractorDispatch extends TitleExtractor {
  protected def fetch(u: String): Option[String] = {
    val req = url(u)
    val future = Http(req OK as.String).option
    future()
  }
}
