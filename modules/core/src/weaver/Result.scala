package weaver

import cats.data.NonEmptyList
import cats.data.Validated.{ Invalid, Valid }

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
      formatError(name, msg, source, location, None)
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

      formatError(name, description, Some(source), location, Some(5))
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

  private def formatError(
      name: String,
      msg: String,
      source: Option[Throwable],
      location: Option[SourceLocation],
      traceLimit: Option[Int]): String = {

    val stackTrace = source.fold("") { ex =>
      val trace: Array[String] = {
        val tr = ex.getStackTrace.map(_.toString)
        traceLimit.fold(Array.empty[String]) { limit =>
          if (tr.length <= limit) tr
          else
            tr.filterNot(_.contains("cats.effect.internals"))
              .filterNot(_.contains("java.util.concurrent"))
              .take(limit) :+ "..."
        }
      }

      formatDescription(trace.mkString("\n"), None, Console.RED, tab4)
    }

    val formattedMessage = formatDescription(
      if (msg != null && msg.nonEmpty) msg else "Test failed",
      location,
      Console.RED,
      tab2
    )

    red("- ") + name + EOL + formattedMessage + stackTrace
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
            s" (${l.fileName.getOrElse("none")}:${l.line})")
        else
          color + prefix + line
    }

    lines.mkString("\n") + Console.RESET
  }
}
