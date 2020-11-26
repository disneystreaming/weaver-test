package weaver

import cats.effect.ContextShift
import cats.Parallel
import cats.effect.Concurrent

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running
 * effect types.
 */
protected[weaver] trait UnsafeRun[F[_]] {

  implicit def concurrent: Concurrent[F]
  implicit def contextShift: ContextShift[F]
  implicit def parallel: Parallel[F]

  def void: F[Unit]

  def background(task: F[Unit]): F[Unit]

  def sync(task: F[Unit]): Unit

}
