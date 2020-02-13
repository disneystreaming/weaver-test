package weaver
package framework
package test

import weaver._
import sbt.testing.Status
import cats.implicits._
import weaver.framework.DogFood

object DogFoodSuite extends SimpleIOSuite with DogFood {

  simpleTest("test suite reports successes events") {
    runSuite(Meta.MutableSuiteTest).map {
      case (_, events) => forall(events)(isSuccess)
    }
  }

  simpleTest(
    "the framework reports exceptions occurring during suite initialisation") {
    runSuite("weaver.framework.test.Meta$CrashingSuite").map {
      case (logs, events) =>
        val errorLogs = logs.collect { case LoggedEvent.Error(msg) => msg }
        exists(events.headOption) { event =>
          val name = event.fullyQualifiedName()
          expect(name == "weaver.framework.test.Meta$CrashingSuite") and
          expect(event.status() == Status.Error)
        } and exists(errorLogs) { log =>
          expect(log.contains("Unexpected failure")) and
          expect(log.contains("Boom"))
        }
    }
  }
}

// The build tool will only detect and run top-level test suites. We can however nest objects
// that contain failing tests, to allow for testing the framework without failing the build
// because the framework will have ran the tests on its own.
object Meta {
  object MutableSuiteTest extends MutableSuiteTest

  object Boom extends Error("Boom") with scala.util.control.NoStackTrace
  object CrashingSuite extends SimpleIOSuite {
    throw Boom
  }
}
