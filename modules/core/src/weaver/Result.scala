package weaver

import cats.data.NonEmptyList
import cats.data.Validated.{Invalid, Valid}

import scala.annotation.tailrec

sealed trait Result {
  def formatted(name: String): String
}

object Result {

  val EOL = java.lang.System.lineSeparator()

  val tab2 = "  "
  val tab4 = "    "

  def fromAssertion(assertion: Expectations): Result = assertion.run match {
    case Valid(_) => Success
    case Invalid(failed) =>
      Failures(failed.map(ex =>
        Result.Failure(ex.message, Some(ex), Some(ex.location))))
  }

  final case object Success extends Result {
    def formatted(name: String): String = {
      green("+ ") + name
    }
  }

  final case class Ignored(reason: Option[String], location: SourceLocation)
      extends Result {

    def formatted(name: String): String = {
      val reasonStr =
        reason.fold("")(msg =>
          formatDescription(msg, Some(location), Console.YELLOW, tab2))
      yellow("- ") + name + yellow(" !!! IGNORED !!!") + EOL + reasonStr
    }
  }

  final case class Cancelled(reason: Option[String], location: SourceLocation)
      extends Result {

    def formatted(name: String): String = {
      val reasonStr =
        reason.fold("")(msg =>
          formatDescription(msg, Some(location), Console.YELLOW, tab2))
      yellow("- ") + name + yellow(" !!! CANCELLED !!!") + EOL + reasonStr
    }
  }

  final case class Failures(failures: NonEmptyList[Failure]) extends Result {

    def formatted(name: String): String =
      if (failures.size == 1) failures.head.formatted(name)
      else {

        val descriptions = failures.zipWithIndex.map {
          case (failure, idx) =>
            import failure._

            formatDescription(
              if (msg != null && msg.nonEmpty) msg else "Test failed",
              location,
              Console.RED,
              s" [$idx] "
            )
        }

        val header = red("- ") + name + EOL
        header + descriptions.toList.mkString("\n\n")
      }
  }

  final case class Failure(
      msg: String,
      source: Option[Throwable],
      location: Option[SourceLocation])
      extends Result {

    def formatted(name: String): String =
      formatError(name, msg, source, location, Some(0))
  }

  final case class Exception(
      source: Throwable,
      location: Option[SourceLocation])
      extends Result {

    def formatted(name: String): String = {
      val description = {
        val name      = source.getClass.getName
        val className = name.substring(name.lastIndexOf(".") + 1)
        Option(source.getMessage)
          .filterNot(_.isEmpty)
          .fold(className)(m => s"$className: $m")
      }

      val stackTraceLimit = if (location.isDefined) Some(10) else None
      formatError(name, description, Some(source), location, stackTraceLimit)
    }
  }

  val success: Result = Success

  def from(error: Throwable): Result = {
    error match {
      case ex: AssertionException =>
        Result.Failure(ex.message, Some(ex), Some(ex.location))
      case ex: IgnoredException =>
        Result.Ignored(ex.reason, ex.location)
      case ex: CanceledException =>
        Result.Cancelled(ex.reason, ex.location)
      case ex: WeaverException =>
        Result.Exception(ex, Some(ex.getLocation))
      case other =>
        Result.Exception(other, None)
    }
  }

  @tailrec
  private def formatStackTraces(accumulator: Seq[String], ex: Throwable, traceLimit: Option[Int]): Seq[String] = {
    val trace: Array[String] = {
      val tr = ex.getStackTrace
        .map(_.toString)
        .filterNot(_.contains("cats.effect.internals"))
        .filterNot(_.contains("java.util.concurrent"))
        .filterNot(_.contains("zio.internal"))
        .filterNot(_.contains("java.lang.Thread"))
      traceLimit.fold(tr) { limit =>
        if (tr.length <= limit) tr
        else if (limit == 0) Array()
        else tr.take(limit) :+ "..."
      }
    }

    Option(ex.getCause) match {
      case None =>
        accumulator ++ trace
      case Some(cause) =>
        val traceCausedBy = accumulator ++ trace ++ Vector("\nCaused by:",  cause.getMessage)
        formatStackTraces(traceCausedBy, cause, traceLimit)
    }
  }

  private def formatError(
      name: String,
      msg: String,
      source: Option[Throwable],
      location: Option[SourceLocation],
      traceLimit: Option[Int]): String = {

    val stackTrace = source.fold("") { ex =>
      val trace = formatStackTraces(Vector.empty, ex, traceLimit)

      if(trace.nonEmpty)
        formatDescription(trace.mkString("\n"), None, Console.RED, tab4)
      else ""
    }

    val formattedMessage = formatDescription(
      if (msg != null && msg.nonEmpty) msg else "Test failed",
      location,
      Console.RED,
      tab2
    )

    var res = red("- ") + name + EOL + formattedMessage +  "\n\n"
    if (stackTrace.nonEmpty){
      res += stackTrace + "\n\n"
    }
    res
  }

  private def formatDescription(
      message: String,
      location: Option[SourceLocation],
      color: String,
      prefix: String): String = {

    val lines = message.split("\\r?\\n").zipWithIndex.map {
      case (line, index) =>
        if (index == 0)
          color + prefix + line +
          location.fold("")(l =>
            s" (${l.bestEffortPath.getOrElse("none")}:${l.line})")
        else
          color + prefix + line
    }

    lines.mkString("\n") + Console.RESET
  }
}
