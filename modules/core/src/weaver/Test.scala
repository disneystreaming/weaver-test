package weaver

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

import cats.Defer
import cats.data.Chain
import cats.syntax.all._

import CECompat.Ref

object Test {

  def apply[F[_]](name: String, f: Log[F] => F[Expectations])(
      implicit F: EffectCompat[F]
  ): F[TestOutcome] = {
    import F.effect
    for {
      ref   <- Ref[F].of(Chain.empty[Log.Entry])
      start <- F.realTimeMillis
      res <- Defer[F]
        .defer(f(Log.collected[F, Chain](ref, F.realTimeMillis)))
        .map(Result.fromAssertion)
        .handleError(ex => Result.from(ex))
      end  <- F.realTimeMillis
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)
  }

  def pure(name: String)(ex: () => Expectations): TestOutcome = {
    val start               = System.currentTimeMillis()
    val (attempt, duration) = Try(ex()) -> (System.currentTimeMillis() - start)

    val res = attempt match {
      case Success(assertions) => Result.fromAssertion(assertions)
      case Failure(ex)         => Result.from(ex)
    }

    TestOutcome(name, duration.millis, res, Chain.empty)
  }

  def apply[F[_]](name: String, f: F[Expectations])(
      implicit F: EffectCompat[F]
  ): F[TestOutcome] = apply(name, (_: Log[F]) => f)

}
