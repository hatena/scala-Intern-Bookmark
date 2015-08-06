package internbookmark.model

import org.joda.time.LocalDateTime

case class Entry(id: Long, url: String, title: String, createdAt: LocalDateTime, updatedAt: LocalDateTime)
