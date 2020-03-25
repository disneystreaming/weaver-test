package weaver
package framework
package test

import cats.implicits._
import sbt.testing.Status
import cats.data.Chain

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
        val errorLogs = extractLogEvent(logs) {
          case LoggedEvent.Error(msg) => msg
        }
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

        val actual = extractLogEvent(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        expectEqual(expected, actual)
    }
  }

  simpleTest("test suite outputs stack traces of exception causes") {
    runSuite(Meta.ErroringWithCauses).map {
      case (logs, _) =>
        val actual = extractLogEvent(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

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
          |  <snipped>                cats.effect.internals.<...>
          |  <snipped>                java.util.concurrent.<...>
          |
          |  Caused by: weaver.framework.test.Meta$CustomException: root
          |
          |  DogFoodTests.scala:15    my.package.MyClass#MyMethod
          |  DogFoodTests.scala:20    my.package.ClassOfDifferentLength#method$new$1
          |  <snipped>                cats.effect.internals.<...>
          |  <snipped>                java.util.concurrent.<...>
          |
          |""".stripMargin.trim

        expectEqual(expected, actual)
    }
  }

  simpleTest("failures with multi-line test name are rendered correctly") {
    runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual = extractLogEvent(logs) {
          case LoggedEvent.Error(msg) => msg
        }.get

        val expected = """
        |- lots
        |  of
        |  multiline
        |  (failure)
        |  assertion failed (src/main/DogFoodTests.scala:5)
        |
        |  expect(1 == 2)
        |
        """.stripMargin.trim

        expectEqual(expected, actual)
    }
  }

  simpleTest("successes with multi-line test name are rendered correctly") {
    runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual =
          extractLogEvent(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(success)") => msg
          }.get

        val expected = """
        |+ lots
        |  of
        |  multiline
        |  (success)
        """.stripMargin.trim

        expectEqual(expected, actual)
    }
  }

  simpleTest("ignored tests with multi-line test name are rendered correctly") {
    runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual =
          extractLogEvent(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(ignored)") => msg
          }.get

        val expected = """
        |- lots
        |  of
        |  multiline
        |  (ignored) !!! IGNORED !!!
        |  Ignore me (src/main/DogFoodTests.scala:5)
        """.stripMargin.trim

        expectEqual(expected, actual)
    }
  }

  simpleTest("cancelled tests with multi-line test name are rendered correctly") {
    runSuite(Meta.Rendering).map {
      case (logs, _) =>
        val actual =
          extractLogEvent(logs) {
            case LoggedEvent.Info(msg) if msg.contains("(cancelled)") => msg
          }.get

        val expected = """
        |- lots
        |  of
        |  multiline
        |  (cancelled) !!! CANCELLED !!!
        |  I was cancelled :( (src/main/DogFoodTests.scala:5)
        """.stripMargin.trim

        expectEqual(expected, actual)
    }
  }

  private def multiLineComparisonReport(expectedS: String, actual: String) = {
    val expectedLines = expectedS.split("\n").map(Option.apply).toVector
    val actualLines   = actual.split("\n").map(Option.apply).toVector

    val lines = expectedLines.size max actualLines.size
    val maxExpectedLineLength = "<missing>".length max expectedLines
      .map(_.map(_.length + 2).getOrElse(0))
      .max
    def padStr(s: String, l: Int) = s + (" " * (l - s.length))

    (expectedLines
      .padTo(lines, None))
      .zip(actualLines.padTo(lines, None))
      .map {
        case (None, Some(actualLine)) =>
          padStr("<missing>", maxExpectedLineLength) + " != " + s"'$actualLine'"
        case (Some(expectedLine), Some(actualLine)) =>
          val op = if (expectedLine == actualLine) "==" else "!="
          padStr(s"'$expectedLine'", maxExpectedLineLength) + s" $op " + s"'$actualLine'"
        case (Some(expectedLine), None) =>
          padStr(s"'$expectedLine'", maxExpectedLineLength) + " != " + s"<missing>"
        case (None, None) => ""
      }
      .mkString("\n")
  }

  private def expectEqual(expected: String, actual: String): Expectations = {
    if (expected != actual) {
      val report = multiLineComparisonReport(expected, actual)
      failure(
        s"Output is not as expected (line-by-line-comparison below): \n\n$report")
    } else
      Expectations.Helpers.success
  }

  private def extractLogEvent(logs: Chain[LoggedEvent])(
      pf: PartialFunction[LoggedEvent, String]): Option[String] = {
    logs
      .collectFirst(pf)
      .map(TestConsole.removeASCIIColors)
      .map(_.trim)
  }
}
