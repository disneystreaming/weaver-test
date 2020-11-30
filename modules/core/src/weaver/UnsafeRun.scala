package weaver

import cats.Parallel
import cats.effect.{ Concurrent, ContextShift, Timer }

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running
 * effect types.
 */
protected[weaver] trait UnsafeRun[F[_]] {

  implicit def effect: Concurrent[F]
  implicit def parallel: Parallel[F]
  implicit def contextShift: ContextShift[F]
  implicit def timer: Timer[F]

  def void: F[Unit]

  def background(task: F[Unit]): F[Unit]

  def sync(task: F[Unit]): Unit

}
