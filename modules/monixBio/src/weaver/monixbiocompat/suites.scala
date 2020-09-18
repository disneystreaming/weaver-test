package weaver
package monixbiocompat

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }
import monix.bio.{ IO, Task }
import monix.execution.Scheduler

trait BaseIOSuite { self: ConcurrentEffectSuite[Task] =>
  val scheduler: Scheduler                    = monix.execution.Scheduler.global
  implicit def timer: Timer[Task]             = IO.timer(scheduler)
  implicit def cs: ContextShift[Task]         = IO.contextShift(scheduler)
  implicit def effect: ConcurrentEffect[Task] = IO.catsEffect(scheduler)
}

trait MutableIOSuite
    extends MutableFSuite[Task]
    with BaseIOSuite
    with Expectations.Helpers

trait SimpleMutableIOSuite extends MutableIOSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure(())
}
