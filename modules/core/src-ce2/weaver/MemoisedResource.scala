package weaver

import cats.effect._
import cats.effect.concurrent.{Deferred, Ref}
import cats.syntax.all._

object MemoisedResource {
  def apply[F[_]: Concurrent, A](
      resource: Resource[F, A]): F[Resource[F, A]] =
    new MemoisedResource[F, A].apply(resource)
}

private class MemoisedResource[F[_]: Concurrent, A] {

  sealed trait State
  case object Uninitialised extends State
  case class InUse(value: Deferred[F, A], finalizer: F[Unit], uses: Int)
      extends State

  def apply(resource: Resource[F, A]): F[Resource[F, A]] =
    Ref[F].of[State](Uninitialised).map { ref =>
      val initialise: F[A] = for {
        valuePromise     <- Deferred[F, A]
        finaliserPromise <- Deferred[F, F[Unit]]
        compute <- ref.modify {
          case Uninitialised =>
            val newState = InUse(valuePromise, finaliserPromise.get.flatten, 1)
            val compute = for {
              (a, fin) <- resource.allocated
              _        <- valuePromise.complete(a)
              _        <- finaliserPromise.complete(fin)
            } yield a
            newState -> compute
          case InUse(value, finalizer, uses) =>
            val newState      = InUse(value, finalizer, uses + 1)
            val compute: F[A] = value.get
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
