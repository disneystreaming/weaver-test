package weaver
package framework
package test

import cats.data.Chain
import cats.implicits._
import sbt.testing.Status

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

  simpleTest("test suite outputs logs for failed tests") {
    runSuite(Meta.FailingSuiteWithlogs).map { case(logs, _) =>
      val logRegex = """^\s+\[(.*?)\]\s+\[.*?\]\s+(.*?)$""".r
      case class LogMessage(level: String, message: String)

      val errorEvents = logs.collect { case LoggedEvent.Error(msg) => msg }

      val logMessages = errorEvents.flatMap(event => Chain(event.lines.toSeq:_*)).collect {
        case logRegex(level, message) => LogMessage(level, message)
      }

      expect(logMessages.map(_.level).distinct == Chain("INFO")) and
      expect(logMessages.map(_.message) == Chain("this test", "has failed"))
    }
  }

  simpleTest("test suite outputs stack traces of exception causes") {
    runSuite(Meta.ErroringWithCauses).map { case(logs, _) =>
      val maybeError = logs.collectFirst { case LoggedEvent.Error(msg) => msg }

      exists(maybeError) { error =>
        expect(error.contains("Exception: 1")) and
          expect(error.contains("Caused by: 2")) and
          expect(error.contains("Caused by: 3"))
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

  object FailingSuiteWithlogs extends SimpleIOSuite {
    loggedTest("failure") { log =>
      for {
        _ <- log.info("this test")
        _ <- log.info("has failed")
      } yield failure("expected")
    }

  }

  object ErroringWithCauses extends SimpleIOSuite {
    loggedTest("erroring with causes") { log =>
      throw new Exception("1", new Exception("2", new Exception("3")))
    }

  }
}
