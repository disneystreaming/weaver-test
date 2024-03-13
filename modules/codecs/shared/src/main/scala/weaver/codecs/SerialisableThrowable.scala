package weaver.codecs

import io.circe._

case class SerialisableThrowable(
    message: String,
    stackTrace: Vector[StackTraceElement],
    cause: Option[SerialisableThrowable]
) extends Throwable {
  override def getMessage(): String          = message
  override def fillInStackTrace(): Throwable = this
  override def getStackTrace()               = stackTrace.toArray
  override def getCause(): Throwable         = cause.orNull
}

object SerialisableThrowable {
  def fromThrowable(throwable: Throwable): SerialisableThrowable =
    throwable match {
      case s: SerialisableThrowable => s
      case other => SerialisableThrowable(
          other.getMessage(),
          other.getStackTrace().toVector,
          Option(other.getCause).map(fromThrowable))
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

  implicit val seEncoder: Encoder[SerialisableThrowable] =
    recursiveEncoder {
      Encoder.forProduct3[SerialisableThrowable,
                          String,
                          Vector[StackTraceElement],
                          Option[SerialisableThrowable]](
        "message",
        "stackTrace",
        "cause")(se => (se.message, se.stackTrace, se.cause))
    }

  implicit val seDecoder: Decoder[SerialisableThrowable] =
    recursiveDecoder {
      Decoder.forProduct3[SerialisableThrowable,
                          String,
                          Vector[StackTraceElement],
                          Option[SerialisableThrowable]](
        "message",
        "stackTrace",
        "cause")(SerialisableThrowable.apply)
    }
}
