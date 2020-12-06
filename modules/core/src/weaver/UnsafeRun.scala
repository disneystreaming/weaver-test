package weaver

import java.util.concurrent.TimeUnit

import cats.Parallel
import cats.effect.{ Concurrent, ContextShift, Timer }

protected[weaver] trait EffectCompat[F[_]] {
  implicit def parallel: Parallel[F]
  implicit def effect: Concurrent[F]
  implicit final def compiler: fs2.Stream.Compiler[F, F] =
    fs2.Stream.Compiler.syncInstance(effect)
  implicit def timer: Timer[F]
  implicit def contextShift: ContextShift[F]
  def realTimeMillis: F[Long]
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

  def realTimeMillis: F[Long] = timer.clock.realTime(TimeUnit.MILLISECONDS)

}
