package weaver
package ziocompat

import zio._
import zio.interop.catz._

object SharedResources extends ZIOGlobalResource {
  def share(store: ZIOGlobalResource.Write): RManaged[ZEnv, Unit] =
    for {
      foo <- ZManaged.succeed("hello world!")
      _   <- store.putM(foo)
    } yield ()
}

class ResourceSharingSuite(global: GlobalResource.Read[T])
    extends ZIOSuite[Has[String]] {

  val sharedLayer: RLayer[ZEnv, Has[String]] =
    ZLayer.fromEffect(global.getOrFail[String]())

  test("a stranger, from the outside ! ooooh") {
    ZIO.access[Has[String]](_.get).map(s => expect(s == "hello world!"))
  }
}

class OtherResourceSharingSuite(global: GlobalResource.Read[T])
    extends ZIOSuite[Has[Option[Int]]] {
  val sharedLayer: RLayer[ZEnv, Has[Option[Int]]] =
    ZLayer.fromEffect(global.get[Int]())

  test("oops, forgot something here") {
    ZIO.access[Has[Option[Int]]](_.get).map(o => expect(o.isEmpty))
  }

}
