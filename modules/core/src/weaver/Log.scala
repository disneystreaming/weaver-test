package weaver

import cats.syntax.all._
import cats.{ Applicative, FlatMap, Monoid, MonoidK, Show, ~> }

import CECompat.Ref

abstract class Log[F[_]: FlatMap](timestamp: F[Long]) { self =>
  def log(l: => Log.Entry): F[Unit]

  final def mapK[G[_]: FlatMap](fk: F ~> G): Log[G] =
    new Log[G](fk(timestamp)) {
      override def log(l: => Log.Entry) = fk(self.log(l))
    }

  final val info =
    new PartiallyAppliedLevel(Log.info)
  final val warn =
    new PartiallyAppliedLevel(Log.warn)
  final val debug =
    new PartiallyAppliedLevel(Log.debug)
  final val error =
    new PartiallyAppliedLevel(Log.error)

  final class PartiallyAppliedLevel(level: Log.Level) {
    def apply(
        msg: => String,
        ctx: Map[String, String] = Map.empty,
        cause: Throwable = null)(
        implicit loc: SourceLocation): F[Unit] =
      FlatMap[F].flatMap(timestamp) { now =>
        self.log(Log.Entry(now, msg, ctx, level, Option(cause), loc))
      }
  }

}

object Log {
  def apply[F[_]](implicit instance: Log[F]): Log[F] = instance

  /**
   * Builds a logger that collects to a referential-transparent variable
   *
   * @param ref
   *   A reference to the logger
   * @tparam F
   *   Effect type
   * @tparam L
   *   Logging collection type
   */
  private[weaver] def collected[F[_]: FlatMap, L[_]: MonoidK: Applicative](
      ref: Ref[F, L[Entry]],
      ts: F[Long]): Log[F] = {
    new Log[F](ts) {
      implicit val monoid: Monoid[L[Entry]] = MonoidK[L].algebra[Entry]

      override def log(entry: => Entry): F[Unit] =
        ref.update(_ |+| entry.pure[L])
    }
  }

  case class Entry(
      timestamp: Long,
      msg: String,
      ctx: Map[String, String],
      level: Log.Level,
      cause: Option[Throwable],
      location: SourceLocation)

  sealed abstract class Level(val label: String)
  case object info  extends Level("info")
  case object warn  extends Level("warn")
  case object debug extends Level("debug")
  case object error extends Level("error")

  object Level {
    val values: List[Level] = List(info, warn, debug, error)

    def fromString(s: String): Either[String, Level] =
      values.find(_.label == s) match {
        case Some(level) => Right(level)
        case None        => Left(s"$s is not a valid log-level")
      }

    implicit val levelShow: Show[Level] = {
      case `info`  => "[INFO]"
      case `warn`  => "[WARN]"
      case `debug` => "[DEBUG]"
      case `error` => "[ERROR]"
    }
  }

}
