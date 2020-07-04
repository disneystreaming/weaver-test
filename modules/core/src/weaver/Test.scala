package weaver

import scala.concurrent.duration.{ MILLISECONDS, _ }

import cats.data.Chain
import cats.effect.concurrent.Ref
import cats.effect.{ Sync, Timer }
import cats.syntax.all._

object Test {

  def apply[F[_]](name: String, f: Log[F] => F[Expectations])(
      implicit F: Sync[F],
      T: Timer[F]
  ): F[TestOutcome] =
    for {
      ref   <- Ref[F].of(Chain.empty[Log.Entry])
      start <- T.clock.realTime(MILLISECONDS)
      res <- Sync[F]
        .defer(f(Log.collected[F, Chain](ref)))
        .map(Result.fromAssertion)
        .handleError(ex => Result.from(ex))
      end  <- T.clock.realTime(MILLISECONDS)
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)

  def apply[F[_]](name: String, f: F[Expectations])(
      implicit F: Sync[F],
      T: Timer[F]
  ): F[TestOutcome] = apply(name, (_: Log[F]) => f)

}
