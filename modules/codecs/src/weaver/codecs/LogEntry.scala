package weaver
package codecs

import io.circe.{ Decoder, Encoder }

/**
 * This is a serialisable version of Log.Entry
 */
case class LogEntry(
    timestamp: Long,
    msg: String,
    ctx: Map[String, String],
    level: Log.Level,
    cause: Option[SerialisableThrowable],
    location: SourceLocation
)

object LogEntry {

  def fromEntry(entry: Log.Entry): LogEntry = LogEntry(
    entry.timestamp,
    entry.msg,
    entry.ctx,
    entry.level,
    entry.cause.map(SerialisableThrowable.fromThrowable),
    entry.location
  )

  implicit val logEntryEncoder: Encoder[LogEntry] =
    Encoder.forProduct6[LogEntry,
                        Long,
                        String,
                        Map[String, String],
                        Log.Level,
                        Option[SerialisableThrowable],
                        SourceLocation]("timestamp",
                                        "msg",
                                        "ctx",
                                        "level",
                                        "cause",
                                        "location") { entry =>
      (entry.timestamp,
       entry.msg,
       entry.ctx,
       entry.level,
       entry.cause,
       entry.location)
    }

  implicit val logEntryDecoder: Decoder[LogEntry] =
    Decoder.forProduct6[LogEntry,
                        Long,
                        String,
                        Map[String, String],
                        Log.Level,
                        Option[SerialisableThrowable],
                        SourceLocation]("timestamp",
                                        "msg",
                                        "ctx",
                                        "level",
                                        "cause",
                                        "location")(LogEntry.apply)
}
