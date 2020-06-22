package weaver
package framework

import cats.effect.IO
import cats.effect.Resource

object Global extends GlobalResourcesInit {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    for {
      foo <- Resource.pure[IO, Int](1)
      _   <- store.putR(foo)
    } yield ()

  case class Foo[F[_]](fint: F[Int])

}
