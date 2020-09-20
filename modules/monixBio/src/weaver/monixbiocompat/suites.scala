package weaver
package monixbiocompat

import scala.concurrent.duration.{ MILLISECONDS, _ }

import cats.data.Chain
import cats.effect.concurrent.Ref
import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }

import monix.bio.{ IO, Task }
import monix.execution.Scheduler

trait BaseIOSuite { self: ConcurrentEffectSuite[Task] =>
  val scheduler: Scheduler                    = monix.execution.Scheduler.global
  implicit def timer: Timer[Task]             = IO.timer(scheduler)
  implicit def cs: ContextShift[Task]         = IO.contextShift(scheduler)
  implicit def effect: ConcurrentEffect[Task] = IO.catsEffect(scheduler)
}

/**
 * Individual test runner for Monix BIO's `IO[Throwable, A]` that properly handles unexpected errors,
 * i.e. errors that occur in another channel.
 */
object Test {
  def apply(
      name: String,
      f: Log[Task] => Task[Expectations]): Task[TestOutcome] =
    for {
      ref   <- Ref[Task].of(Chain.empty[Log.Entry])
      start <- IO.clock.realTime(MILLISECONDS)
      res <- IO
        .defer(f(Log.collected[Task, Chain](ref)))
        .map(Result.fromAssertion)
        .redeemCause(c => Result.from(c.toThrowable), identity)
      end  <- IO.clock.realTime(MILLISECONDS)
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)

  def apply(name: String, f: Task[Expectations]): Task[TestOutcome] =
    apply(name, (_: Log[Task]) => f)
}

trait MutableIOSuite
    extends MutableFSuite[Task]
    with BaseIOSuite
    with Expectations.Helpers {
  override def test(name: String): PartiallyAppliedTest =
    new SubPartiallyAppliedTest(name)

  class SubPartiallyAppliedTest(name: String)
      extends super.PartiallyAppliedTest(name) {
    override def apply(run: => Task[Expectations]): Unit =
      registerTest(name)(_ => Test(name, run))
    override def apply(run: Res => Task[Expectations]): Unit =
      registerTest(name)(res => Test(name, run(res)))
    override def apply(run: (Res, Log[Task]) => Task[Expectations]): Unit =
      registerTest(name)(res => Test(name, log => run(res, log)))
  }
}

trait SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure(())
}
