package weaver
package framework
package test

import java.time.OffsetDateTime

import cats.effect.{ Clock, IO, Timer }
import cats.implicits._
import sbt.testing.Status

import scala.concurrent.duration.{ FiniteDuration, TimeUnit }

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
    runSuite(Meta.FailingSuiteWithlogs).map {
      case (logs, _) =>
        val expected =
          s"""
            |- failure
            |  expected (src/main/DogFoodTests.scala:5)
            |
            |    [INFO]  12:54:35 [DogFoodTests.scala:5] this test
            |    [ERROR] 12:54:35 [DogFoodTests.scala:5] has failed
            |    [DEBUG] 12:54:35 [DogFoodTests.scala:5] with context
            |        a       -> b
            |        token   -> <something>
            |        request -> true
            |""".stripMargin.trim

        val errorEvents =
          logs
            .collectFirst { case LoggedEvent.Error(msg) => msg }
            .map(TestConsole.removeASCIIColors)
            .map(_.trim)

        expect(errorEvents.contains(expected))
    }
  }

  simpleTest("test suite outputs stack traces of exception causes") {
    runSuite(Meta.ErroringWithCauses).map {
      case (logs, _) =>
        val maybeError = logs
          .collectFirst { case LoggedEvent.Error(msg) => msg }
          .map(TestConsole.removeASCIIColors)
          .map(_.trim)

        val expected =
          """
          |- erroring with causes
          |  Meta$CustomException: surfaced error
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |
          |  Caused by: weaver.framework.test.Meta$CustomException: first cause
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |  <snipped>                                   cats.effect.internals.<...>
          |  <snipped>                                    java.util.concurrent.<...>
          |
          |  Caused by: weaver.framework.test.Meta$CustomException: root
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |  <snipped>                                   cats.effect.internals.<...>
          |  <snipped>                                    java.util.concurrent.<...>
          |
          |""".stripMargin.trim

        expect(maybeError.contains(expected))
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
