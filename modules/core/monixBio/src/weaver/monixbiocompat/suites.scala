package weaver
package monixbiocompat

import scala.concurrent.duration.{ MILLISECONDS, _ }

import cats.data.Chain
import cats.effect.{ Resource }
import cats.effect.concurrent.Ref

import monix.bio.{ IO, Task }
import monix.execution.Scheduler

trait BaseIOSuite extends EffectSuite[Task]

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
      start <- ts
      res <- IO
        .defer(f(Log.collected[Task, Chain](ref, ts)))
        .map(Result.fromAssertion)
        .redeemCause(c => Result.from(c.toThrowable), identity)
      end  <- ts
      logs <- ref.get
    } yield TestOutcome(name, (end - start).millis, res, logs)

  private val ts = IO.clock.realTime(MILLISECONDS)

  def apply(name: String, f: Task[Expectations]): Task[TestOutcome] =
    apply(name, (_: Log[Task]) => f)
}

abstract class MutableIOSuite
    extends MutableFSuite[Task]
    with BaseIOSuite
    with Expectations.Helpers {

  implicit protected def effectCompat               = MonixBIOUnsafeRun
  final implicit protected def scheduler: Scheduler = effectCompat.scheduler

  override def test(name: TestName): PartiallyAppliedTest =
    new SubPartiallyAppliedTest(name)

  class SubPartiallyAppliedTest(name: TestName)
      extends super.PartiallyAppliedTest(name) {
    override def apply(run: => Task[Expectations]): Unit =
      registerTest(name)(_ => Test(name.name, run))
    override def apply(run: Res => Task[Expectations]): Unit =
      registerTest(name)(res => Test(name.name, run(res)))
    override def apply(run: (Res, Log[Task]) => Task[Expectations]): Unit =
      registerTest(name)(res => Test(name.name, log => run(res, log)))
  }
}

abstract class SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure[Task, Unit](())
}

trait FunIOSuite
    extends FunSuiteF[Task]
    with BaseIOSuite
    with Expectations.Helpers {
  implicit protected def effectCompat               = MonixBIOUnsafeRun
  final implicit protected def scheduler: Scheduler = effectCompat.scheduler
}
