package weaver

import java.util.concurrent.TimeUnit

import cats.effect.Timer
import cats.effect.concurrent.Ref
import cats.implicits._
import cats.{ Applicative, FlatMap, Monoid, MonoidK, Show, ~> }

import weaver.Log.PartiallyAppliedLevel

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

  sealed trait Level
  case object info  extends Level
  case object warn  extends Level
  case object debug extends Level
  case object error extends Level

  object Level {
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
        timer: Timer[F],
        F: FlatMap[F]): F[Unit] =
      F.flatMap(timer.clock.realTime(TimeUnit.MILLISECONDS)) { now =>
        Log[F].log(Entry(now, msg, ctx, level, Option(cause), loc))
      }
  }

}
