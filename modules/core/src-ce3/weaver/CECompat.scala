package weaver

import cats.effect.Async
import cats.effect.syntax.all._
import cats.syntax.all._

private[weaver] object CECompat extends CECompat

private[weaver] trait CECompat {

  private[weaver] type Effect[F[_]] = Async[F]

  private[weaver] type Ref[F[_], A] = cats.effect.kernel.Ref[F, A]
  private[weaver] val Ref = cats.effect.kernel.Ref

  private[weaver] type Semaphore[F[_]] = cats.effect.std.Semaphore[F]
  private[weaver] val Semaphore = cats.effect.std.Semaphore

  private[weaver] def guaranteeCase[F[_]: Async, A](
      fa: F[A])(
      cancelled: => F[Unit],
      completed: => F[Unit],
      errored: Throwable => F[Unit]): F[A] =
    Async[F].guaranteeCase(fa)(_.fold(cancelled, errored, _ *> completed))

  private[weaver] def guarantee[F[_]: Async, A](
      fa: F[A])(fin: F[Unit]): F[A] =
    Async[F].guarantee(fa, fin)

  private[weaver] def background[F[_]: Async, A, B](fa: F[A], default: A)(
      f: F[A] => F[B]): F[B] =
    fa.background.use(fOutcome =>
      f(fOutcome.flatMap(_.embed(onCancel = Async[F].pure(default)))))

}
