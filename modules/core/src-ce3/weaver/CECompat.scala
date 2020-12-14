package weaver

import cats.effect.Async
import cats.syntax.all._

object CECompat extends CECompat

trait CECompat {

  protected[weaver] type Effect[F[_]] = Async[F]

  protected[weaver] type Ref[F[_], A] = cats.effect.kernel.Ref[F, A]
  protected[weaver] val Ref = cats.effect.kernel.Ref

  protected[weaver] type Semaphore[F[_]] = cats.effect.std.Semaphore[F]
  protected[weaver] val Semaphore = cats.effect.std.Semaphore

  def guaranteeCase[F[_]: Async, A](
      fa: F[A])(
      cancelled: => F[Unit],
      completed: => F[Unit],
      errored: Throwable => F[Unit]): F[A] =
    Async[F].guaranteeCase(fa)(_.fold(cancelled, errored, _ *> completed))

  def guarantee[F[_]: Async, A](
      fa: F[A])(fin: F[Unit]): F[A] =
    Async[F].guarantee(fa, fin)

}
