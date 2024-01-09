package weaver

object LogFormatter {
  def formatTimestamp(l: Long): String = internals.Timestamp.format(l)
}
