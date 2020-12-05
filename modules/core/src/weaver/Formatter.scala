package weaver

import scala.concurrent.duration.FiniteDuration

import cats.data.Chain
import cats.syntax.show._

import weaver.Log.{ debug, error, info, warn }

import Colours._
import LogFormatter.{ formatTimestamp }

object Formatter {
  val EOL        = java.lang.System.lineSeparator()
  val DOUBLE_EOL = EOL * 2

  sealed abstract class Tabulation(val prefix: String) {
    override def toString = prefix
  }
  case object TAB2 extends Tabulation("  ")
  case object TAB4 extends Tabulation("    ")

  def formatResultStatus(
      name: String,
      result: Result,
      d: FiniteDuration): String = {
    val tabulatedTestLines = name.split("\\r?\\n").map(TAB2 -> _).toList

    def withDuration(l: String) = l + " " + whitebold(renderDuration(d))

    def withPrefix(newPrefix: String): String = {
      tabulatedTestLines match {
        case (_, firstLine) :: Nil => newPrefix + withDuration(firstLine)
        case (_, firstLine) :: extraLines =>
          newPrefix + withDuration(firstLine) + EOL + extraLines
            .map(l => l._1.prefix + l._2)
            .mkString(EOL)
        case Nil => newPrefix + ""
      }
    }

    import Result._

    result match {
      case Success                                 => withPrefix(green("+ "))
      case _: Failure | _: Failures | _: Exception => withPrefix(red("- "))
      case _: Cancelled =>
        withPrefix(yellow("- ")) + yellow(" !!! CANCELLED !!!")
      case _: Ignored => withPrefix(yellow("- ")) + yellow(" !!! IGNORED !!!")
    }
  }

  def outcomeWithResult(
      outcome: TestOutcome,
      result: Result,
      mode: TestOutcome.Mode): String = {

    import outcome._
    import TestOutcome.{ Verbose, Summary }

    val builder = new StringBuilder()
    val newLine = '\n'
    builder.append(formatResultStatus(name, result, outcome.duration))

    if ((mode == Verbose && outcome.status.isFailed) || (mode == Summary && !outcome.status.isFailed))
      result.formatted.foreach { resultInfo =>
        builder.append(EOL)
        builder.append(resultInfo)
      }
    if (status.isFailed && mode == Verbose) {
      val hasDebugOrError =
        log.exists(e => List(debug, error).contains(e.level))
      val shortLevelPadder = if (hasDebugOrError) "  " else " "
      val levelPadder: Log.Level => String = {
        case `info` | `warn`   => shortLevelPadder
        case `debug` | `error` => " "
      }

      val eff = log.map { entry =>
        builder.append(TAB4)
        val loc = entry.location.fileName
          .map(fn => s"[$fn:${entry.location.line}]")
          .getOrElse("")

        builder.append(s"${entry.level.show}${levelPadder(entry.level)}")
        builder.append(s"${formatTimestamp(entry.timestamp)} ")
        builder.append(s"$loc ")
        builder.append(entry.msg)
        val keyLengthMax =
          entry.ctx.map(_._1.length).foldLeft[Int](0)(math.max)

        entry.ctx.foreach {
          case (k, v) =>
            builder.append(newLine)
            builder.append(TAB4.prefix * 2)
            builder.append(k)
            (0 to (keyLengthMax - k.length)).foreach(_ => builder.append(" "))
            builder.append("-> ")
            builder.append(v)
        }
        builder.append(newLine)

        ()
      }

      discard[Chain[Unit]](eff)
      if (log.nonEmpty) {
        builder.append(newLine)
      }
    }
    builder.mkString
  }

  def renderDuration(d: FiniteDuration) = {
    val millis  = d.toMillis
    val seconds = d.toSeconds

    if (millis < 1000) s"${millis}ms"
    else if (seconds >= 1 && seconds < 60)
      s"${seconds}s" // less than 60 seconds
    else {
      val fullMinutes = seconds / 60
      val remSeconds  = seconds - fullMinutes

      if (remSeconds == 0) {
        s"${fullMinutes}min"
      } else {
        s"${fullMinutes}:${remSeconds}min"
      }
    }
  }
}
