package internbookmark.helper

import internbookmark.repository

object EntriesForTest extends repository.Entries {
  import repository.TitleExtractor
  override def titleExtractor: TitleExtractor = new TitleExtractor {
    def fetch(url: String): Option[String] = Some("<title>test-title</title>")
  }
}
