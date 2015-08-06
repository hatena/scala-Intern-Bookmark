package internbookmark.model

import org.joda.time.LocalDateTime

case class Bookmark(id: Long, user: User, entry: Entry, comment: String, createdAt: LocalDateTime, updatedAt: LocalDateTime)
