package weaver
package ziocompat

import zio._

object SharedResources extends ZIOGlobalResource {
  def share(global: GlobalWrite): RManaged[ZEnv, Unit] =
    for {
      foo <- ZManaged.succeed("hello world!")
      _   <- global.putM(foo)
    } yield ()
}

class ResourceSharingSuite(global: GlobalRead) extends ZIOSuite[Has[String]] {

  val sharedLayer: RLayer[ZEnv, Has[String]] =
    global.getLayer[String]()

  test("a stranger, from the outside ! ooooh") {
    ZIO.access[Has[String]](_.get).map(s => expect(s == "hello world!"))
  }
}

class OtherResourceSharingSuite(global: GlobalRead)
    extends ZIOSuite[Has[Option[Int]]] {
  val sharedLayer: RLayer[ZEnv, Has[Option[Int]]] =
    ZLayer.fromEffect(global.get[Int]())

  test("oops, forgot something here") {
    ZIO.access[Has[Option[Int]]](_.get).map(o => expect(o.isEmpty))
  }

}
