package weaver
package monixcompat

import cats.effect.{ ConcurrentEffect, ContextShift, Resource, Timer }

import monix.eval.Task
import monix.eval.instances._
import monix.execution.Scheduler

trait BaseTaskSuite { self: ConcurrentEffectSuite[Task] =>
  val scheduler: Scheduler            = monix.execution.Scheduler.global
  implicit def timer: Timer[Task]     = Task.timer(scheduler)
  implicit def cs: ContextShift[Task] = Task.contextShift(scheduler)
  implicit def effect: ConcurrentEffect[Task] =
    new CatsConcurrentEffectForTask()(scheduler, Task.defaultOptions)
}

trait MutableTaskSuite extends MutableFSuite[Task] with BaseTaskSuite

trait SimpleMutableTaskSuite extends MutableTaskSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure(())
}
