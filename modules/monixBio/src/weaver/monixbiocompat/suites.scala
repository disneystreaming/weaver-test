package weaver
package monixbiocompat

import scala.concurrent.duration.{ MILLISECONDS, _ }

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }

import monix.bio.{ IO, Task }
import monix.execution.Scheduler

trait BaseIOSuite { self: ConcurrentEffectSuite[Task] =>
  val scheduler: Scheduler                    = monix.execution.Scheduler.global
  implicit def timer: Timer[Task]             = IO.timer(scheduler)
  implicit def cs: ContextShift[Task]         = IO.contextShift(scheduler)
  implicit def effect: ConcurrentEffect[Task] = IO.catsEffect(scheduler)
}

import cats.data.Chain
import cats.effect.concurrent.Ref
import cats.syntax.all._

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
        .redeemCauseWith(c => IO.raiseError(c.toThrowable), IO.now)
        .handleError(ex => Result.from(ex))
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
    new PartiallyAppliedTest {
      val testName: String = name
      override def apply(run: => Task[Expectations]): Unit =
        registerTest(testName)(_ => Test(testName, run))
      override def apply(run: Res => Task[Expectations]): Unit =
        registerTest(testName)(res => Test(testName, run(res)))
      override def apply(run: (Res, Log[Task]) => Task[Expectations]): Unit =
        registerTest(testName)(res => Test(testName, log => run(res, log)))
    }
}

trait SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure(())
}
