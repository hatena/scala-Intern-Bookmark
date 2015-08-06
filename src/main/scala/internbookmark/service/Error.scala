package internbookmark.service

sealed trait Error {
  override def toString(): String = this match {
    case BookmarkNotFoundError => "Can not find a target bookmark."
    case EntryNotFoundError => "Can not find a target entry."
  }
}
final case object BookmarkNotFoundError extends Error
final case object EntryNotFoundError extends Error

