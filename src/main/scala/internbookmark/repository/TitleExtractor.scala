package internbookmark.repository

trait TitleExtractor {
  protected def fetch(url: String): Option[String]
  def extract(url: String): Option[String] =
    fetch(url).flatMap { content =>
      """<title>(.+?)</title>""".r.findFirstMatchIn(content).map(_.group(1))
    }
}
