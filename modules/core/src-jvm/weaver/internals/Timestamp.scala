package weaver.internals

import java.time.format.DateTimeFormatter
import java.time.{ Instant, OffsetDateTime, ZoneId }

private[weaver] object Timestamp {

  def format(l: Long): String = {
    val current =
      OffsetDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault())

    current.withNano(0).format(DateTimeFormatter.ISO_LOCAL_TIME)
  }

  def localTime(hours: Int, minutes: Int, seconds: Int): Long = {
    java.time.OffsetDateTime.now
      .withHour(hours)
      .withMinute(minutes)
      .withSecond(seconds)
      .toEpochSecond * 1000

  }
}
