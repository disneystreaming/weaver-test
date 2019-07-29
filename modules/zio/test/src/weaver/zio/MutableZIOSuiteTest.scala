package weaver.ziocompat

import zio.duration._

object MutableZIOSuiteTest extends SimpleMutableZIOSuite {

  test("hello test") {
    for {
      _ <- zio.clock.sleep(1.seconds)
      _ <- log.info("hello")
    } yield expect(List(1, 2, 3).size == 3)
  }

  pureTest("hello pure") {
    expect(true)
  }

}
