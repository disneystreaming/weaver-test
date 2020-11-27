package weaver
package framework
package test

import cats.effect.{ IO, Resource }

object SharedResources extends GlobalResourcesInit[IO] {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    for {
      foo <- Resource.pure[IO, String]("hello world!")
      _   <- store.putR(foo)
    } yield ()
}

class ResourceSharingSuite(globalResources: GlobalResources.Read[IO])
    extends IOSuite {
  type Res = String
  def sharedResource: Resource[IO, String] =
    globalResources.getOrFailR[String]()

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}

class OtherResourceSharingSuite(globalResources: GlobalResources.Read[IO])
    extends IOSuite {
  type Res = Option[Int]
  def sharedResource: Resource[IO, Option[Int]] =
    globalResources.getR[Int]()

  test("oops, forgot something here") { sharedInt =>
    IO(expect(sharedInt.isEmpty))
  }

}
