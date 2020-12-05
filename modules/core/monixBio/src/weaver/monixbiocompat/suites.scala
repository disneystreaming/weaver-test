package weaver
package monixbiocompat

import scala.concurrent.duration.{ MILLISECONDS, _ }

import cats.Parallel
import cats.data.Chain
import cats.effect.concurrent.Ref
import cats.effect.{ Concurrent, ContextShift, Resource, Timer }

import monix.bio.{ IO, Task }
import monix.execution.Scheduler

object MonixBioUnsafeRun extends UnsafeRun[Task] {
  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  implicit val effect: Concurrent[monix.bio.Task] = IO.catsEffect(scheduler)
  implicit val parallel: Parallel[monix.bio.Task] = IO.catsParallel
  implicit val contextShift: ContextShift[monix.bio.Task] =
    IO.contextShift(scheduler)
  implicit val timer: Timer[monix.bio.Task] = IO.timer(scheduler)
  def void: monix.bio.Task[Unit]            = IO.unit
  def background(task: monix.bio.Task[Unit]): monix.bio.Task[Unit] = {
    val cancelToken = task.runAsync { _ => () }(scheduler)
    monix.bio.Task(cancelToken.cancel())
  }
  def sync(task: monix.bio.Task[Unit]): Unit  = PlatformCompat.runSync(task)
  def async(task: monix.bio.Task[Unit]): Unit = task.runAsyncAndForget
}

trait BaseIOSuite extends RunnableSuite[Task] {
  override val unsafeRun = MonixBioUnsafeRun

  implicit protected def scheduler: Scheduler = unsafeRun.scheduler
  implicit protected def timer: Timer[Task]   = unsafeRun.timer
  implicit protected def contextShift: ContextShift[Task] =
    unsafeRun.contextShift
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

trait SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure[Task, Unit](())
}
