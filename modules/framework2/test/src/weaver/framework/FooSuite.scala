package weaver
package framework

import cats.effect.{ IO, Resource }
import scala.concurrent.duration._

object FooSuite extends IOSuite {

  override type Res = String
  override def sharedResource: Resource[IO, String] = {
    Resource.make(IO(scala.util.Random.nextString(5)))(s =>
      IO(println(s"Releasing $s")))
  }

  for (i <- 1 to 5) {
    test(s"Test $i") { res =>
      val time = scala.util.Random.nextInt(50)
      IO.sleep(time.millis) *> IO {
        expect(i % 2 == 0)
      }
    }
  }

}

object FooSuite2 extends SimpleIOSuite {

  test("success") {
    success
  }

  test("failure") {
    expect(2 != 2)
  }

}

object FooGlobal extends IOGlobalResourcesInit {
  def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
    store.putR("hello world")
}

class FooUsingGlobal(read: GlobalResources.Read[IO]) extends IOSuite {

  type Res = String

  override def sharedResource: Resource[IO, String] = read.getOrFailR[String]()

  test("global resources still work") { res =>
    IO(expect(res == "hello world"))
  }

}
