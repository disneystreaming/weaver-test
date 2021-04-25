package weaver
package ziocompat

import cats.Parallel
import cats.effect.Async

import zio._
import zio.interop.catz

object ZIOUnsafeRun extends UnsafeRun[T] {

  type CancelToken = Fiber.Id => Exit[Throwable, Unit]

  implicit val runtime = Runtime.default

  implicit def effect: Async[T] = catz.asyncInstance[ZEnv]

  implicit def parallel: Parallel[T] =
    catz.core.parallelInstance[ZEnv, Throwable]

  def background(task: T[Unit]): CancelToken =
    runtime.unsafeRunAsyncCancelable(task)(_ => ())
  def cancel(token: Fiber.Id => Exit[Throwable, Unit]): Unit =
    discard[Exit[Throwable, Unit]](token(Fiber.Id.None))

  def sync[A](task: T[A]): A = runtime.unsafeRun(task)

  def async(task: T[Unit]): Unit = runtime.unsafeRunAsync(task)(_ => ())
}
