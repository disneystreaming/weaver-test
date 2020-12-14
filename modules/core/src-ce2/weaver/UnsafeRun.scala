package weaver

import java.util.concurrent.TimeUnit

import scala.concurrent.duration.FiniteDuration

import cats.Parallel
import cats.effect.{ Async, Blocker, Concurrent, ContextShift, Resource, Timer }

protected[weaver] trait EffectCompat[F[_]] {
  implicit def parallel: Parallel[F]
  implicit def effect: Concurrent[F]
  implicit def timer: Timer[F]
  implicit def contextShift: ContextShift[F]

  def realTimeMillis: F[Long]                  = timer.clock.realTime(TimeUnit.MILLISECONDS)
  def sleep(duration: FiniteDuration): F[Unit] = timer.sleep(duration)
  def fromFuture[A](thunk: => scala.concurrent.Future[A]): F[A] =
    Async.fromFuture(effect.delay(thunk))
  def async[A](cb: (Either[Throwable, A] => Unit) => Unit): F[A] =
    effect.async(cb)

  private[weaver] def blocker[T](
      f: BlockerCompat[F] => T): Resource[F, T] =
    Blocker[F].map(blocker =>
      new BlockerCompat[F] {
        def block[A](thunk: => A): F[A] = blocker.delay(thunk)
      }).map(f)
}

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running
 * effect types.
 */
protected[weaver] trait UnsafeRun[F[_]] extends EffectCompat[F] {

  type CancelToken

  def background(task: F[Unit]): CancelToken
  def cancel(token: CancelToken): Unit

  def sync(task: F[Unit]): Unit
  def async(task: F[Unit]): Unit

}
