package weaver

import io.circe.Json
import io.circe.Encoder
import io.circe.Decoder
import scala.concurrent.duration._
import io.circe.DecodingFailure

package object codecs {

  def eventJson(event: SuiteEvent): String =
    SuiteEvent.suiteEventEncoder(event).noSpaces

  implicit class EncoderOps[A](encoder: Encoder[A]) {
    def oneOf[Union >: A](label: String, value: A): EncodeOneOf[Union, A] =
      EncodeOneOf(label, encoder, value)
  }

  def encodeUnion[Union](
      route: Union => EncodeOneOf[Union, _]): Encoder[Union] =
    (member: Union) => route(member).encode

  def decodeUnion[Union](oneOfs: DecodeOneOf[Union, _]*): Decoder[Union] = {
    val emptyUnion = Decoder.failed[Union](DecodingFailure("Empty union", Nil))
    oneOfs.map(_.upcast).fold(emptyUnion)(_ or _)
  }

  implicit val stackTraceElementEncoder: Encoder[StackTraceElement] =
    (ste: StackTraceElement) =>
      Json.obj(
        "fileName"   -> Json.fromString(ste.getFileName()),
        "className"  -> Json.fromString(ste.getClassName()),
        "methodName" -> Json.fromString(ste.getMethodName()),
        "lineNumber" -> Json.fromInt(ste.getLineNumber())
      )

  implicit val stackTraceElementDecoder: Decoder[StackTraceElement] =
    Decoder.forProduct4[StackTraceElement, String, String, String, Int](
      "fileName",
      "className",
      "methodName",
      "lineNumber") {
      case (fileName, className, methodName, lineNumber) =>
        new StackTraceElement(className, methodName, fileName, lineNumber)
    }

  implicit val throwableEncoder: Encoder[Throwable] = (t: Throwable) =>
    Json.obj(
      "message" -> Json.fromString(t.getMessage()),
      "stackTrace" -> Json.fromValues(
        t.getStackTrace().map(stackTraceElementEncoder.apply))
    )

  implicit val throwableDecoder: Decoder[Throwable] =
    Decoder.forProduct2[Throwable, String, Array[StackTraceElement]](
      "message",
      "stackTrace") {
      case (message, stackTrace) =>
        new Throwable(message) {
          override def fillInStackTrace(): Throwable = this
          override def getStackTrace()               = stackTrace
        }
    }

  implicit val sourceLocationEncoder: Encoder[SourceLocation] =
    Encoder.forProduct3[SourceLocation, Option[String], Option[String], Int](
      "fileName",
      "filePath",
      "line"
    )(sl => (sl.fileName, sl.filePath, sl.line))

  implicit val sourceLocationDecoder: Decoder[SourceLocation] =
    Decoder.forProduct3[SourceLocation, Option[String], Option[String], Int](
      "fileName",
      "filePath",
      "line"
    )(SourceLocation.apply)

  implicit val logLevelEncoder: Encoder[Log.Level] =
    Encoder.encodeString.contramap(level => level.label)

  implicit val logLevelDecoder: Decoder[Log.Level] =
    Decoder.decodeString.emap(Log.Level.fromString)

  implicit val logEntryEncoder: Encoder[Log.Entry] =
    Encoder.forProduct6[Log.Entry,
                        Long,
                        String,
                        Map[String, String],
                        Log.Level,
                        Option[Throwable],
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

  implicit val logEntryDecoder: Decoder[Log.Entry] =
    Decoder.forProduct6[Log.Entry,
                        Long,
                        String,
                        Map[String, String],
                        Log.Level,
                        Option[Throwable],
                        SourceLocation]("timestamp",
                                        "msg",
                                        "ctx",
                                        "level",
                                        "cause",
                                        "location")(Log.Entry.apply)

  implicit val testStatusEncoder: Encoder[TestStatus] =
    Encoder.encodeString.contramap(_.label)

  implicit val testStatusDecoder: Decoder[TestStatus] =
    Decoder.decodeString.emap(TestStatus.fromString)

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.encodeLong.contramap(_.toMillis)

  implicit val finiteDurationDecoder: Decoder[FiniteDuration] =
    Decoder.decodeLong.map(_.millis)

}
