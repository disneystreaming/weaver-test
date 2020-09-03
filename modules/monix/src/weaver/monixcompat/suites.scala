package weaver
package monixcompat

import cats.effect.Resource

import monix.eval.Task
import monix.eval.instances._
import monix.execution.Scheduler
import cats.effect.{ Timer, ContextShift, ConcurrentEffect }

trait BaseMonixSuite { self : ConcurrentEffectSuite[Task] =>
  val ec = scala.concurrent.ExecutionContext.global
  val scheduler = Scheduler(ec)
  implicit def timer : Timer[Task] = Task.timer(scheduler)
  implicit def cs : ContextShift[Task] = Task.contextShift(scheduler)
  implicit def effect : ConcurrentEffect[Task] =
    new CatsConcurrentEffectForTask()(scheduler, Task.defaultOptions)
}

trait MutableMonixSuite extends MutableFSuite[Task] with BaseMonixSuite

trait SimpleMutableMonixSuite extends MutableMonixSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure(())
}
