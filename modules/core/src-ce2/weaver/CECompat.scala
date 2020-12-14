package weaver

import cats.effect.Concurrent
import cats.effect.ExitCase.Canceled
import cats.effect.ExitCase.Completed

object CECompat extends CECompat

trait CECompat {

  protected[weaver] type Effect[F[_]] = Concurrent[F]
  protected[weaver] type Ref[F[_], A] = cats.effect.concurrent.Ref[F, A]

  protected[weaver] val Ref = cats.effect.concurrent.Ref

  def guaranteeCase[F[_]: Concurrent, A](
      fa: F[A])(
      cancelled: => F[Unit],
      completed: => F[Unit],
      errored: Throwable => F[Unit]): F[A] =
    Concurrent[F].guaranteeCase(fa) {
      case Canceled                      => cancelled
      case Completed                     => completed
      case cats.effect.ExitCase.Error(e) => errored(e)
    }

  def guarantee[F[_]: Concurrent, A](
      fa: F[A])(fin: F[Unit]): F[A] =
    Concurrent[F].guarantee(fa)(fin)

}
