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

  for (i <- 1 to 20) {
    test(s"Test $i") { res =>
      val time = scala.util.Random.nextInt(50)
      IO.sleep(time.millis) *> IO {
        if (i % 2 == 0) expect(1 == 2)
        else succeed("yep")
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
