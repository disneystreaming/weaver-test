package weaver
package monixcompat

import cats.effect.{ ContextShift, Timer }

import monix.eval.Task
import monix.execution.Scheduler

object MonixUnsafeRun extends UnsafeRun[Task] {

  type CancelToken = Task[Unit]

  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  override implicit val contextShift: ContextShift[Task] =
    Task.contextShift(scheduler)
  override implicit val timer: Timer[Task] =
    Task.timer(scheduler)

  override implicit val effect   = Task.catsEffect(scheduler)
  override implicit val parallel = Task.catsParallel

  def void: Task[Unit] = Task.unit

  def background(task: Task[Unit]): CancelToken = {
    val cancelToken = task.runAsync { _ => () }(scheduler)
    Task(cancelToken.cancel())
  }

  def cancel(token: CancelToken): Unit = sync(token)

  def sync(task: Task[Unit]): Unit = PlatformCompat.runSync(task)

  def async(task: Task[Unit]): Unit = task.runAsyncAndForget

}
