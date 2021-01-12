package weaver

import cats.effect._
import cats.syntax.all._

import CECompat.{ Deferred, Ref }

object MemoisedResource {
  def apply[F[_]: Concurrent, A](
      resource: Resource[F, A]): F[Resource[F, A]] =
    new MemoisedResource[F, A].apply(resource)
}

private class MemoisedResource[F[_]: Concurrent, A] {

  sealed trait State
  case object Uninitialised extends State
  case class InUse(
      value: Deferred[F, Either[Throwable, A]],
      finalizer: F[Unit],
      uses: Int)
      extends State

  def apply(resource: Resource[F, A]): F[Resource[F, A]] =
    Ref[F].of[State](Uninitialised).map { ref =>
      val initialise: F[A] = for {
        valuePromise     <- Deferred[F, Either[Throwable, A]]
        finaliserPromise <- Deferred[F, F[Unit]]
        compute <- ref.modify {
          case Uninitialised =>
            val newState = InUse(valuePromise, finaliserPromise.get.flatten, 1)
            val compute = Concurrent[F].attempt(resource.allocated).flatMap {
              case Right((a, fin)) => for {
                  _ <- valuePromise.complete(Right(a))
                  _ <- finaliserPromise.complete(fin)
                } yield a
              case Left(e) => for {
                  _ <- valuePromise.complete(Left(e))
                  _ <- finaliserPromise.complete(Concurrent[F].unit)
                  _ <- ref.set(Uninitialised) // reset state
                  a <- Concurrent[F].raiseError[A](e)
                } yield a
            }
            newState -> compute
          case InUse(value, finalizer, uses) =>
            val newState      = InUse(value, finalizer, uses + 1)
            val compute: F[A] = value.get.flatMap(Concurrent[F].fromEither)
            newState -> compute
        }
        value <- compute
      } yield value

      val finalise: F[Unit] = ref.modify[F[Unit]] {
        case Uninitialised =>
          Uninitialised -> Concurrent[F].raiseError(
            new IllegalStateException("Implementation error"))
        case InUse(_, finaliser, n) if n <= 1 =>
          Uninitialised -> finaliser
        case InUse(value, finaliser, n) =>
          InUse(value, finaliser, n - 1) -> Concurrent[F].unit
      }.flatten

      Resource.make(initialise)(_ => finalise)
    }

}
