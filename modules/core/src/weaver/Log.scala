package weaver

import cats.implicits._
import cats.effect.concurrent.Ref
import cats.{ Applicative, Monoid, MonoidK, Show, ~> }
import weaver.Log.PartiallyAppliedLevel

abstract class Log[F[_]] { self =>
  def log(l: => Log.Entry): F[Unit]

  final def mapK[G[_]](fk: F ~> G): Log[G] = new Log[G] {
    override def log(l: => Log.Entry) = fk(self.log(l))
  }

  final val info  = new PartiallyAppliedLevel[F](Log.info)(self)
  final val warn  = new PartiallyAppliedLevel[F](Log.warn)(self)
  final val debug = new PartiallyAppliedLevel[F](Log.debug)(self)
  final val error = new PartiallyAppliedLevel[F](Log.error)(self)
}

object Log {

  def apply[F[_]](implicit instance: Log[F]): Log[F] = instance

  // Logger that doesn't do anything
  def nop[F[_]: Applicative]: Log[F] = _ => Applicative[F].unit

  // Logger that collects the entries in a referential-transparent variable
  // containing a collection
  def collected[F[_], L[_]: MonoidK: Applicative](
      ref: Ref[F, L[Entry]]): Log[F] = new Log[F] {
    implicit val monoid: Monoid[L[Entry]] = MonoidK[L].algebra[Entry]

    override def log(entry: => Entry): F[Unit] = ref.update(_ |+| entry.pure[L])
  }

  case class Entry(
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
      case `info`  => "[INFO] "
      case `warn`  => "[WARN] "
      case `debug` => "[DEBUG]"
      case `error` => "[ERROR]"
    }
  }

  implicit class PartiallyAppliedLevel[F[_]](level: Level)(
      implicit log: Log[F]) {
    def apply(
        msg: => String,
        ctx: Map[String, String] = Map.empty,
        cause: Throwable = null)(implicit loc: SourceLocation): F[Unit] =
      Log[F].log(Entry(msg, ctx, level, Option(cause), loc))
  }

}
