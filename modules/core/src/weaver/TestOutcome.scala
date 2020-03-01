package weaver

import cats.data.Chain
import cats.implicits._

import scala.concurrent.duration.FiniteDuration
import LogFormatter.{ formatTimestamp }
import weaver.Log.{ debug, error, info, warn }

case class TestOutcome(
    name: String,
    duration: FiniteDuration,
    result: Result,
    log: Chain[Log.Entry]) {

  def isFailed: Boolean = result match {
    case Result.Success | Result.Cancelled(_, _) | Result.Ignored(_, _) => false
    case Result.Failure(_, _, _) | Result.Failures(_)                   => true
    case Result.Exception(_, _)                                         => true
  }

  def formatted: String = {
    val builder = new StringBuilder()
    val newLine = '\n'
    builder.append(result.formatted(name))
    if (isFailed) {
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
        builder.append(newLine)
        val keyLengthMax = entry.ctx.map(_._1.length).foldLeft[Int](0)(math.max)
        entry.ctx.foreach {
          case (k, v) =>
            builder.append(newLine)
            builder.append(Result.tab4 + Result.tab4)
            builder.append(k)
            (0 to (keyLengthMax - k.length)).foreach(builder.append(" "))
            builder.append("-> ")
            builder.append(v)
        }
      }
      discard[Chain[Unit]](eff)
      if (log.nonEmpty) {
        builder.append(newLine)
      }
    }
    builder.mkString
  }

}
