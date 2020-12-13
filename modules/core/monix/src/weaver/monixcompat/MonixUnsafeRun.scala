package weaver
package monixcompat

import cats.effect.{ ContextShift, Timer }

import monix.eval.Task
import monix.execution.{ Cancelable, Scheduler }

object MonixUnsafeRun extends UnsafeRun[Task] {

  type CancelToken = Cancelable

  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  override implicit val contextShift: ContextShift[Task] =
    Task.contextShift(scheduler)
  override implicit val timer: Timer[Task] =
    Task.timer(scheduler)

  override implicit val effect   = Task.catsEffect(scheduler)
  override implicit val parallel = Task.catsParallel

  def background(task: Task[Unit]): Cancelable =
    task.runAsync { _ => () }(scheduler)

  def cancel(token: CancelToken): Unit = token.cancel()

  def sync(task: Task[Unit]): Unit = PlatformCompat.runSync(task)

  def async(task: Task[Unit]): Unit = task.runAsyncAndForget

}
