package weaver

import cats.data.Chain
import cats.implicits._

import scala.concurrent.duration.FiniteDuration
import LogFormatter.{ formatTimestamp }
import weaver.Log.{ debug, error, info, warn }

trait TestOutcome {
  def name: String
  def duration: FiniteDuration
  def status: Status
  def log: Chain[Log.Entry]
  def formatted: String
  def cause: Option[Throwable]
}

object TestOutcome {

  def apply(
      name: String,
      duration: FiniteDuration,
      result: Result,
      log: Chain[Log.Entry]): TestOutcome = Default(name, duration, result, log)

  case class Default(
      name: String,
      duration: FiniteDuration,
      result: Result,
      log: Chain[Log.Entry])
      extends TestOutcome {

    def status: Status = result match {
      case Result.Success                               => Status.Success
      case Result.Cancelled(_, _)                       => Status.Cancelled
      case Result.Ignored(_, _)                         => Status.Ignored
      case Result.Failure(_, _, _) | Result.Failures(_) => Status.Failure
      case Result.Exception(_, _)                       => Status.Exception
    }

    def cause: Option[Throwable] = result match {
      case Result.Exception(cause, _)       => Some(cause)
      case Result.Failure(_, maybeCause, _) => maybeCause
      case _                                => None
    }

    def formatted: String = {
      val builder = new StringBuilder()
      val newLine = '\n'
      builder.append(result.formatted(name))
      if (status.isFailed) {
        val hasDebugOrError =
          log.exists(e => List(debug, error).contains(e.level))
        val shortLevelPadder = if (hasDebugOrError) "  " else " "
        val levelPadder: Log.Level => String = {
          case `info` | `warn`   => shortLevelPadder
          case `debug` | `error` => " "
        }

        val eff = log.map { entry =>
          builder.append(Result.tab4)
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
              builder.append(Result.tab4.prefix * 2)
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

  }
}
