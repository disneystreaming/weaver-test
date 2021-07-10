package weaver
package framework
package test

import cats.effect.{ IO, Resource}

object SharedResources extends IOGlobalResource {
  class RefFactory(init: Int){
    val generate: Resource[IO, CECompat.Ref[IO, Int]] = Resource.eval(CECompat.Ref.of[IO, Int](init))
  }

  def sharedResources(global: GlobalResourceF.Write[IO]): Resource[IO, Unit] =
    for {
      foo <- Resource.pure[IO, String]("hello world!")
      bar <- Resource.pure[IO, RefFactory](new RefFactory(0))
      _   <- global.putR(foo)
      _   <- global.putR(bar)
    } yield ()
}

class ResourceSharingSuite(globalResources: GlobalResourceF.Read[IO])
    extends IOSuite {
  type Res = String
  def sharedResource: Resource[IO, String] =
    globalResources.getOrFailR[String]()

  test("a stranger, from the outside ! ooooh") { sharedString =>
    IO(expect(sharedString == "hello world!"))
  }
}

class OtherResourceSharingSuite(globalResources: GlobalResourceF.Read[IO])
    extends IOSuite {
  type Res = Option[Int]
  def sharedResource: Resource[IO, Option[Int]] =
    globalResources.getR[Int]()

  test("oops, forgot something here") { sharedInt =>
    IO(expect(sharedInt.isEmpty))
  }

}

class UniqueResourceSuite(global: GlobalRead) extends IOForEachSuite{
  type Res = CECompat.Ref[IO, Int]
  def uniqueResource = global.getOrFailR[SharedResources.RefFactory]().flatMap(_.generate)

  override val maxParallelism = 1

  test("start value is set to 0") { refIO =>
    refIO.get.map(i => expect(i == 0))
  }
  test("value is updated locally to 1") { refIO=>
    refIO.update(_ + 1) *>
    refIO.get.map(i => expect(i == 1))
  }
  test("value is still 0 in another test") { refIO=>
    refIO.get.map(i => expect(i == 0))
  }
}

class SharedResourceSuite(global: GlobalRead) extends IOSuite{
  type Res = CECompat.Ref[IO, Int]
  def sharedResource = global.getOrFailR[SharedResources.RefFactory]().flatMap(_.generate)

  override val maxParallelism = 1

  test("start value is set to 0") { refIO =>
    refIO.get.map(i => expect(i == 0))
  }
  test("value is updated in whole suit to 1") { refIO=>
    refIO.update(_ + 1) *>
      refIO.get.map(i => expect(i == 1))
  }
  test("value is changed to 1 in another test") { refIO=>
    refIO.get.map(i => expect(i == 1))
  }
}