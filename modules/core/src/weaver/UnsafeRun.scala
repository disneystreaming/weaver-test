package weaver

import cats.Parallel
import cats.effect.{ ConcurrentEffect, ContextShift, Timer }

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running
 * effect types.
 */
protected[weaver] trait UnsafeRun[F[_]] {

  type CancelToken

  implicit def effect: ConcurrentEffect[F]
  implicit def parallel: Parallel[F]
  implicit def contextShift: ContextShift[F]
  implicit def timer: Timer[F]

  def void: F[Unit]

  def background(task: F[Unit]): CancelToken
  def cancel(token: CancelToken): Unit

  def sync(task: F[Unit]): Unit
  def async(task: F[Unit]): Unit

}
