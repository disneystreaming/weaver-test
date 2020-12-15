package weaver

import cats.effect.Concurrent
import cats.effect.ExitCase.{ Canceled, Completed }

private[weaver] object CECompat extends CECompat

private[weaver] trait CECompat {

  private[weaver] type Effect[F[_]] = Concurrent[F]
  private[weaver] type Ref[F[_], A] = cats.effect.concurrent.Ref[F, A]
  private[weaver] val Ref = cats.effect.concurrent.Ref

  private[weaver] type Semaphore[F[_]] = cats.effect.concurrent.Semaphore[F]
  private[weaver] val Semaphore = cats.effect.concurrent.Semaphore

  private[weaver] def guaranteeCase[F[_]: Concurrent, A](
      fa: F[A])(
      cancelled: => F[Unit],
      completed: => F[Unit],
      errored: Throwable => F[Unit]): F[A] =
    Concurrent[F].guaranteeCase(fa) {
      case Canceled                      => cancelled
      case Completed                     => completed
      case cats.effect.ExitCase.Error(e) => errored(e)
    }

  private[weaver] def guarantee[F[_]: Concurrent, A](
      fa: F[A])(fin: F[Unit]): F[A] =
    Concurrent[F].guarantee(fa)(fin)

}
