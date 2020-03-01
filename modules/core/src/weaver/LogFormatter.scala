package weaver

import java.time.format.DateTimeFormatter
import java.time.{ Instant, ZoneId }

object LogFormatter {
  def formatTimestamp(l: Long): String = {
    Instant
      .ofEpochMilli(l)
      .atZone(ZoneId.systemDefault())
      .toLocalTime
      .withNano(0)
      .format(DateTimeFormatter.ISO_LOCAL_TIME)
  }
}
