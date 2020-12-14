package weaver

import cats.syntax.all._
import cats.{ Applicative, Monoid, MonoidK, Show, ~> }

import weaver.Log.PartiallyAppliedLevel

import CECompat.Ref

abstract class Log[F[_]] { self =>
  def log(l: => Log.Entry): F[Unit]

  final def mapK[G[_]](fk: F ~> G): Log[G] = new Log[G] {
    override def log(l: => Log.Entry) = fk(self.log(l))
  }

  final val info =
    new PartiallyAppliedLevel[F](Log.info)(self)
  final val warn =
    new PartiallyAppliedLevel[F](Log.warn)(self)
  final val debug =
    new PartiallyAppliedLevel[F](Log.debug)(self)
  final val error =
    new PartiallyAppliedLevel[F](Log.error)(self)
}

object Log {
  def apply[F[_]](implicit instance: Log[F]): Log[F] = instance

  // Logger that doesn't do anything
  def nop[F[_]: Applicative]: Log[F] = _ => Applicative[F].unit

  /**
   * Builds a logger that collects to a referential-transparent variable
   *
   * @param ref A reference to the logger
   * @tparam F Effect type
   * @tparam L Logging collection type
   */
  def collected[F[_], L[_]: MonoidK: Applicative](
      ref: Ref[F, L[Entry]]): Log[F] = new Log[F] {
    implicit val monoid: Monoid[L[Entry]] = MonoidK[L].algebra[Entry]

    override def log(entry: => Entry): F[Unit] = ref.update(_ |+| entry.pure[L])
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

  implicit class PartiallyAppliedLevel[F[_]](level: Level)(
      implicit log: Log[F]) {
    def apply(
        msg: => String,
        ctx: Map[String, String] = Map.empty,
        cause: Throwable = null)(
        implicit loc: SourceLocation,
        F: EffectCompat[F]): F[Unit] =
      F.effect.flatMap(F.realTimeMillis) { now =>
        Log[F].log(Entry(now, msg, ctx, level, Option(cause), loc))
      }
  }

}
