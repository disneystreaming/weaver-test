package weaver

import cats.Applicative
import cats.effect.ExitCase.{ Canceled, Completed }
import cats.effect.syntax.all._
import cats.effect.{ Concurrent, Resource }
import cats.syntax.all._

private[weaver] object CECompat extends CECompat

private[weaver] trait CECompat {

  private[weaver] type Effect[F[_]] = Concurrent[F]
  private[weaver] type Ref[F[_], A] = _root_.cats.effect.Ref[F, A]
  private[weaver] val Ref = _root_.cats.effect.Ref

  private[weaver] type Deferred[F[_], A] = _root_.cats.effect.Deferred[F, A]
  private[weaver] val Deferred = _root_.cats.effect.Deferred

  private[weaver] type Semaphore[F[_]] = cats.effect.std.Semaphore[F]
  private[weaver] val Semaphore = cats.effect.std.Semaphore

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
    Concurrent[F].guarantee(fa, fin)

  private[weaver] def background[F[_]: Concurrent, A, B](fa: F[A], default: A)(
      f: F[A] => F[B]): F[B] =
    fa.background.use(f)

  private[weaver] def resourceLift[F[_]: Applicative, A](
      fa: F[A]): Resource[F, A] = Resource.eval(fa)

  private[weaver] trait Queue[F[_], A] {
    protected def fs2Queue: fs2.concurrent.Queue[F, A]

    def enqueue(a: A): F[Unit]          = fs2Queue.enqueue1(a)
    def dequeueStream: fs2.Stream[F, A] = fs2Queue.dequeue
  }

  private[weaver] object Queue {
    def unbounded[F[_]: Concurrent, A] =
      fs2.concurrent.Queue.unbounded[F, A].map {
        q =>
          new Queue[F, A] {
            override val fs2Queue = q
          }
      }
  }
}
