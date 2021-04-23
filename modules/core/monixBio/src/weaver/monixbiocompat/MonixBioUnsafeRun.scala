package weaver
package monixbiocompat

import cats.Parallel
import cats.effect.{ ContextShift, Timer }

import monix.bio.IO
import monix.execution.{ Cancelable, Scheduler }

object MonixBIOUnsafeRun extends UnsafeRun[monix.bio.Task] {
  type CancelToken = Cancelable
  implicit val scheduler: Scheduler = monix.execution.Scheduler.global

  implicit val effect                             = IO.catsEffect(scheduler)
  implicit val parallel: Parallel[monix.bio.Task] = IO.catsParallel
  implicit val contextShift: ContextShift[monix.bio.Task] =
    IO.contextShift(scheduler)
  implicit val timer: Timer[monix.bio.Task] = IO.timer(scheduler)
  def background(task: monix.bio.Task[Unit]): CancelToken = {
    task.runAsync { _ => () }(scheduler)
  }
  def sync(task: monix.bio.Task[Unit]): Unit  = PlatformCompat.runSync(task)
  def async(task: monix.bio.Task[Unit]): Unit = task.runAsyncAndForget
  def cancel(token: CancelToken): Unit        = token.cancel()

  final val name = "monix-bio"
}
