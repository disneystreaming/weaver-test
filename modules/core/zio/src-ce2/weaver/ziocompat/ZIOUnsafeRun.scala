package weaver
package ziocompat

import cats.Parallel
import cats.effect.{ ConcurrentEffect, ContextShift, Timer }

import zio._
import zio.interop.catz

object ZIOUnsafeRun extends UnsafeRun[T] {

  type CancelToken = Fiber.Id => Exit[Throwable, Unit]

  implicit val runtime = Runtime.default

  implicit def timer: Timer[T]             = catz.zioTimer[ZEnv, Throwable]
  implicit def effect: ConcurrentEffect[T] = catz.taskEffectInstance[ZEnv]

  implicit def parallel: Parallel[T] =
    catz.core.parallelInstance[ZEnv, Throwable]

  implicit def contextShift: ContextShift[T] =
    catz.zioContextShift[ZEnv, Throwable]

  def background(task: T[Unit]): CancelToken =
    runtime.unsafeRunAsyncCancelable(task)(_ => ())
  def cancel(token: Fiber.Id => Exit[Throwable, Unit]): Unit =
    discard[Exit[Throwable, Unit]](token(Fiber.Id.None))

  def sync(task: T[Unit]): Unit = runtime.unsafeRun(task)

  def async(task: T[Unit]): Unit = runtime.unsafeRunAsync(task)(_ => ())
}
