package weaver.test

import weaver._
import cats.implicits._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit

abstract class MutableSuiteTest extends SimpleIOSuite {

  simpleTest("23 is odd") {
    expect(23 % 2 == 1)
  }

  simpleTest("sleeping") {
    for {
      before <- timer.clock.realTime(TimeUnit.MILLISECONDS)
      _      <- timer.sleep(1.seconds)
      _      <- expect(true).failFast
      after  <- timer.clock.realTime(TimeUnit.MILLISECONDS)
    } yield expect(after - before >= 1000)
  }

  simpleTest("23 is odd") {
    expect(true)
  }

  loggedTest("logged") { log =>
    log.info("hello").as(success)
  }

}

object MutableSuiteTest extends MutableSuiteTest
