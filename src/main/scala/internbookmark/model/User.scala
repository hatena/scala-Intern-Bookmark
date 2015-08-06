package internbookmark.model

import org.joda.time.LocalDateTime

case class User(id: Long, name: String, createdAt: LocalDateTime, updatedAt: LocalDateTime)
