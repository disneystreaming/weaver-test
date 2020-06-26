package weaver
package framework
package test

import cats.effect._

import scala.concurrent.duration.{ TimeUnit, FiniteDuration }
import java.time.OffsetDateTime

// The build tool will only detect and run top-level test suites. We can however nest objects
// that contain failing tests, to allow for testing the framework without failing the build
// because the framework will have ran the tests on its own.
object Meta {
  object MutableSuiteTest extends MutableSuiteTest

  object Boom extends Error("Boom") with scala.util.control.NoStackTrace
  object CrashingSuite extends SimpleIOSuite {
    throw Boom
  }

  object Rendering extends SimpleIOSuite {
    implicit val sourceLocation = TimeCop.sourceLocation

    simpleTest("lots\nof\nmultiline\n(success)") {
      expect(1 == 1)
    }

    simpleTest("lots\nof\nmultiline\n(failure)") {
      expect(1 == 2)
    }

    simpleTest("lots\nof\nmultiline\n(ignored)") {
      ignore("Ignore me")
    }

    simpleTest("lots\nof\nmultiline\n(cancelled)") {
      cancel("I was cancelled :(")
    }
  }

  object FailingTestStatusReporting extends SimpleIOSuite {
    simpleTest("I succeeded") {
      success
    }

    simpleTest("I failed") {
      failure(":(")
    }

    simpleTest("I succeeded again") {
      success
    }
  }

  object FailingSuiteWithlogs extends SimpleIOSuite {
    loggedTest("failure") { log =>
      implicit val timer          = TimeCop.setTimer
      implicit val sourceLocation = TimeCop.sourceLocation

      val context = Map(
        "a"       -> "b",
        "token"   -> "<something>",
        "request" -> "true"
      )

      for {
        _ <- log.info("this test")
        _ <- log.error("has failed")
        _ <- log.debug("with context", context)
      } yield failure("expected")
    }

  }

  object ErroringWithCauses extends SimpleIOSuite {
    loggedTest("erroring with causes") { log =>
      throw CustomException(
        "surfaced error",
        CustomException("first cause",
                        CustomException("root", withSnips = true),
                        withSnips = true))
    }
  }

  case class CustomException(
      str: String,
      causedBy: Exception = null,
      withSnips: Boolean = false)
      extends Exception(str, causedBy) {

    val SnippedStackTrace = Array[StackTraceElement](
      new StackTraceElement("cats.effect.internals.IORuntime",
                            "run",
                            "IORuntime.scala",
                            5),
      new StackTraceElement("java.util.concurrent.Thread",
                            "execute",
                            "Thread.java",
                            45)
    )

    val preset = Array(
      new StackTraceElement("my.package.MyClass",
                            "MyMethod",
                            "DogFoodTests.scala",
                            15),
      new StackTraceElement("my.package.ClassOfDifferentLength",
                            "method$new$1",
                            "DogFoodTests.scala",
                            20)
    )

    override def getStackTrace: Array[StackTraceElement] =
      if (withSnips) preset ++ SnippedStackTrace else preset

  }

  object TimeCop {
    private val setTimestamp = OffsetDateTime.now
      .withHour(12)
      .withMinute(54)
      .withSecond(35)
      .toEpochSecond * 1000

    implicit val setClock = new Clock[IO] {
      override def realTime(unit: TimeUnit): IO[Long] = IO(setTimestamp)

      override def monotonic(unit: TimeUnit): IO[Long] = ???
    }

    implicit val setTimer: Timer[IO] = new Timer[IO] {
      override def clock: Clock[IO] = setClock

      override def sleep(duration: FiniteDuration): IO[Unit] = ???
    }

    implicit val sourceLocation: SourceLocation = SourceLocation(
      Some("DogFoodTests.scala"),
      Some("src/main/DogFoodTests.scala"),
      5)
  }

}
