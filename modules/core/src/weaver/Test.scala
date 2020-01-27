package weaver

import cats.syntax.all._
import cats.data.Chain
import cats.effect.concurrent.Ref

import scala.concurrent.duration.{ MILLISECONDS, _ }
import cats.effect.Sync
import cats.effect.Timer

class Test[F[_]](val name: String, val f: Log[F] => F[Expectations]) {

  def compile(
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
}

object Test {

  def apply[F[_]](name: String)(f: Log[F] => F[Expectations]): Test[F] =
    new Test(name, f)

}
