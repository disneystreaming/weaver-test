package weaver
package framework
package test

import cats.effect._

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
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

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
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

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
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun
    implicit val sourceLocation: SourceLocation = TimeCop.sourceLocation

    loggedTest("failure") { log =>
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
    override implicit protected def effectCompat: UnsafeRun[IO] =
      SetTimeUnsafeRun

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
    implicit val sourceLocation: SourceLocation = SourceLocation(
      "src/main/DogFoodTests.scala",
      "src/main/DogFoodTests.scala",
      5)
  }

  object SetTimeUnsafeRun extends CatsUnsafeRun {
    private val setTimestamp = java.time.OffsetDateTime.now
      .withHour(12)
      .withMinute(54)
      .withSecond(35)
      .toEpochSecond * 1000

    override def realTimeMillis: IO[Long] = IO.pure(setTimestamp)
  }

}
