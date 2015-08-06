package internbookmark.service

import org.joda.time.LocalDateTime
import org.json4s._

object Json {
  val Formats = DefaultFormats + LocalDateTimeSerializer + LongIdSerializer
}

case object LongIdSerializer extends CustomSerializer[Long](format => (
  { case JString(s) => s.toLong         },
  { case x: Long => JString(x.toString) }
))

case object LocalDateTimeSerializer extends CustomSerializer[LocalDateTime](format => (
  {
    case JInt(s) => new LocalDateTime(s.toLong)
    case JNull => null
  },
  {
    case d: LocalDateTime => JInt(BigInt(d.toDateTime().getMillis))
  }
))
