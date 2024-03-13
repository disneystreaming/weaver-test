package weaver
package framework
package test

import scala.concurrent.duration._

abstract class MutableSuiteTest extends SimpleIOSuite {

  pureTest("23 is odd") {
    expect(23 % 2 == 1)
  }

  test("sleeping") {
    for {
      before <- CatsUnsafeRun.realTimeMillis
      _      <- CatsUnsafeRun.sleep(1.seconds)
      after  <- CatsUnsafeRun.realTimeMillis
    } yield expect(after - before >= 1000)
  }

  pureTest("23 is odd") {
    expect(23 % 2 == 1)
  }

  loggedTest("logged") { log =>
    log.info("hello").as(success)
  }
}

object MutableSuiteTest extends MutableSuiteTest
