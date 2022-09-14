package weaver

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

import cats.Parallel
import cats.effect.{ Async, Resource }
import cats.syntax.all._

trait EffectCompat[F[_]] {
  implicit def parallel: Parallel[F]
  implicit def effect: Async[F]
  def realTimeMillis: F[Long]                  = effect.realTime.map(_.toMillis)
  def sleep(duration: FiniteDuration): F[Unit] = effect.sleep(duration)
  def fromFuture[A](thunk: => scala.concurrent.Future[A]): F[A] =
    effect.fromFuture(effect.delay(thunk))
  def async[A](cb: (Either[Throwable, A] => Unit) => Unit): F[A] =
    effect.async_(cb)

  private[weaver] def blocker[T](
      f: BlockerCompat[F] => T): Resource[F, T] =
    Resource.pure(f(new BlockerCompat[F] {
      def block[A](thunk: => A): F[A] = effect.blocking(thunk)
    }))

}

/**
 * Abstraction allowing for running IO constructs unsafely.
 *
 * This is meant to delegate to library-specific constructs for running effect
 * types.
 */
trait UnsafeRun[F[_]] extends EffectCompat[F] {

  type CancelToken

  def background(task: F[Unit]): CancelToken
  def cancel(token: CancelToken): Unit

  def unsafeRunSync(task: F[Unit]): Unit
  def unsafeRunAndForget(task: F[Unit]): Unit
  def unsafeRunToFuture(task: F[Unit]): Future[Unit]

}
