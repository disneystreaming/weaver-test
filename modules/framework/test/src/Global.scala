package weaver
package framework

import cats.effect.IO
import cats.effect.Resource

object SharedResources extends GlobalResourcesInit {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    for {
      foo <- Resource.pure[IO, String]("hello world!")
      _   <- store.putR(foo)
    } yield ()
}

class ResourceSharingSuite(globalResources: GlobalResources) extends IOSuite {
  type Res = String
  def sharedResource: Resource[IO, String] =
    globalResources.in[IO].getOrFailR[String]()

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}

class OtherResourceSharingSuite(globalResources: GlobalResources)
    extends IOSuite {
  type Res = Option[Int]
  def sharedResource: Resource[IO, Option[Int]] =
    globalResources.in[IO].getR[Int]()

  test("oops, forgot something here") { sharedInt =>
    IO(expect(sharedInt.isEmpty))
  }

}
