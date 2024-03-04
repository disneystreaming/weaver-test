package weaver
package junit

import cats.Show
import cats.effect._

import org.junit.runner.Description
import org.junit.runner.notification.{ Failure, RunListener, RunNotifier }

object JUnitRunnerTests extends IOSuite {

  type Res = BlockerCompat[IO]
  def sharedResource: Resource[IO, Res] = effectCompat.blocker(identity)

  implicit val showNotifList: Show[List[Notification]] =
    list => list.map(_.toString()).mkString("\n")

  test("Notifications are issued correctly") { blocker =>
    run(blocker, Meta.MySuite).map { notifications =>
      val (failures, filteredNotifs) = notifications.partition {
        case TestFailure(_, _) => true
        case _                 => false
      }
      val failureMessage = failures.collect {
        case TestFailure(_, message) => message
      }

      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$MySuite$"),
        TestStarted("success(weaver.junit.Meta$MySuite$)"),
        TestFinished("success(weaver.junit.Meta$MySuite$)"),
        TestStarted("failure(weaver.junit.Meta$MySuite$)"),
        TestFinished("failure(weaver.junit.Meta$MySuite$)"),
        TestIgnored("ignore(weaver.junit.Meta$MySuite$)"),
        TestSuiteFinished("weaver.junit.Meta$MySuite$")
      )
      expect.same(filteredNotifs, expected) and
        exists(failureMessage)(s =>
          expect(s.contains("oops")) && expect(
            s.contains("Meta.scala")))
    }
  }

  test("Only tests tagged with only are ran") { blocker =>
    run(blocker, Meta.Only).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$Only$"),
        TestIgnored("not only(weaver.junit.Meta$Only$)"),
        TestStarted("only(weaver.junit.Meta$Only$)"),
        TestFinished("only(weaver.junit.Meta$Only$)"),
        TestSuiteFinished("weaver.junit.Meta$Only$")
      )
      expect.same(notifications, expected)
    }
  }

  test("Tests tagged with only fail when ran on CI") { blocker =>
    run(blocker, Meta.OnlyFailsOnCi).map { notifications =>
      def testFailure(name: String, lineNumber: Int) = {
        val srcPath  = "modules/framework-cats/jvm/src/test/scala/junit/Meta.scala"
        val msgLine1 = s"- $name 0ms"
        val msgLine2 =
          s"  'Only' tag is not allowed when `isCI=true` ($srcPath:$lineNumber)"
        TestFailure(
          name = name + "(weaver.junit.Meta$OnlyFailsOnCi$)",
          message =
            s"$msgLine1\n$msgLine2\n\n"
        )
      }
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$OnlyFailsOnCi$"),
        TestIgnored("normal test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestIgnored("not only(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestStarted("first only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        testFailure("first only test", 46),
        TestFinished("first only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestStarted("second only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        testFailure("second only test", 50),
        TestFinished("second only test(weaver.junit.Meta$OnlyFailsOnCi$)"),
        TestSuiteFinished("weaver.junit.Meta$OnlyFailsOnCi$")
      )
      expect.same(notifications, expected)
    }
  }

  test("Only tests tagged with only are ran (unless also tagged ignored)") {
    blocker =>
      run(blocker, Meta.IgnoreAndOnly).map { notifications =>
        val expected = List(
          TestSuiteStarted("weaver.junit.Meta$IgnoreAndOnly$"),
          TestIgnored("only and ignored(weaver.junit.Meta$IgnoreAndOnly$)"),
          TestIgnored("is ignored(weaver.junit.Meta$IgnoreAndOnly$)"),
          TestIgnored("not tagged(weaver.junit.Meta$IgnoreAndOnly$)"),
          TestStarted("only(weaver.junit.Meta$IgnoreAndOnly$)"),
          TestFinished("only(weaver.junit.Meta$IgnoreAndOnly$)"),
          TestSuiteFinished("weaver.junit.Meta$IgnoreAndOnly$")
        )
        expect.same(notifications, expected)
      }
  }

  test("Tests tagged with ignore are ignored") { blocker =>
    run(blocker, Meta.Ignore).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$Ignore$"),
        TestIgnored("is ignored(weaver.junit.Meta$Ignore$)"),
        TestStarted("not ignored 1(weaver.junit.Meta$Ignore$)"),
        TestFinished("not ignored 1(weaver.junit.Meta$Ignore$)"),
        TestStarted("not ignored 2(weaver.junit.Meta$Ignore$)"),
        TestFinished("not ignored 2(weaver.junit.Meta$Ignore$)"),
        TestSuiteFinished("weaver.junit.Meta$Ignore$")
      )
      expect.same(notifications, expected)
    }
  }

  test("Tests tagged with ignore are ignored (FunSuite)") { blocker =>
    runPure(blocker, Meta.IgnorePure).map { notifications =>
      val expected = List(
        TestSuiteStarted("weaver.junit.Meta$IgnorePure$"),
        TestIgnored("is ignored(weaver.junit.Meta$IgnorePure$)"),
        TestStarted("not ignored 1(weaver.junit.Meta$IgnorePure$)"),
        TestFinished("not ignored 1(weaver.junit.Meta$IgnorePure$)"),
        TestStarted("not ignored 2(weaver.junit.Meta$IgnorePure$)"),
        TestFinished("not ignored 2(weaver.junit.Meta$IgnorePure$)"),
        TestSuiteFinished("weaver.junit.Meta$IgnorePure$")
      )
      expect.same(notifications, expected)
    }
  }

  test(
    "Even if all tests are ignored, will fail if a test is tagged with only") {
    blocker =>
      run(blocker, Meta.OnlyFailsOnCiEvenIfIgnored).map { notifications =>
        def testFailure(name: String, lineNumber: Int) = {
          val srcPath  = "modules/framework-cats/jvm/src/test/scala/junit/Meta.scala"
          val msgLine1 = s"- $name 0ms"
          val msgLine2 =
            s"  'Only' tag is not allowed when `isCI=true` ($srcPath:$lineNumber)"
          TestFailure(
            name = name + "(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)",
            message =
              s"$msgLine1\n$msgLine2\n\n"
          )
        }
        val expected = List(
          TestSuiteStarted("weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$"),
          TestIgnored(
            "only and ignored(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)"),
          TestStarted(
            "only and ignored(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)"),
          testFailure("only and ignored", 110),
          TestFinished(
            "only and ignored(weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$)"),
          TestSuiteFinished("weaver.junit.Meta$OnlyFailsOnCiEvenIfIgnored$")
        )
        expect.same(notifications, expected)
      }
  }

  test("Works when suite asks for global resources") {
    blocker =>
      run(blocker, classOf[Meta.Sharing]).map { notifications =>
        val expected = List(
          TestSuiteStarted("weaver.junit.Meta$Sharing"),
          TestStarted("foo(weaver.junit.Meta$Sharing)"),
          TestFinished("foo(weaver.junit.Meta$Sharing)"),
          TestSuiteFinished("weaver.junit.Meta$Sharing")
        )
        expect.same(notifications, expected)
      }
  }

  def run(
      blocker: BlockerCompat[IO],
      suite: Class[_]): IO[List[Notification]] = for {
    runner   <- IO(new WeaverRunner(suite))
    queue    <- IO(scala.collection.mutable.Queue.empty[Notification])
    notifier <- IO(new RunNotifier())
    _        <- IO(notifier.addListener(new NotificationListener(queue)))
    _        <- blocker.block(runner.run(notifier))
  } yield queue.toList

  def run(
      blocker: BlockerCompat[IO],
      suite: SimpleIOSuite): IO[List[Notification]] =
    run(blocker, suite.getClass())

  def runPure(
      blocker: BlockerCompat[IO],
      suite: FunSuite): IO[List[Notification]] =
    run(blocker, suite.getClass())

  sealed trait Notification
  case class TestSuiteStarted(name: String)             extends Notification
  case class TestAssumptionFailure(failure: Failure)    extends Notification
  case class TestFailure(name: String, message: String) extends Notification
  case class TestFinished(name: String)                 extends Notification
  case class TestIgnored(name: String)                  extends Notification
  case class TestStarted(name: String)                  extends Notification
  case class TestSuiteFinished(name: String)            extends Notification

  class NotificationListener(
      queue: scala.collection.mutable.Queue[Notification])
      extends RunListener {
    override def testSuiteStarted(description: Description): Unit =
      queue += TestSuiteStarted(description.getDisplayName())
    override def testAssumptionFailure(failure: Failure): Unit =
      queue += TestAssumptionFailure(failure)
    override def testFailure(failure: Failure): Unit =
      queue += TestFailure(failure.getDescription.getDisplayName,
                           failure.getMessage())
    override def testFinished(description: Description): Unit =
      queue += TestFinished(description.getDisplayName())
    override def testIgnored(description: Description): Unit =
      queue += TestIgnored(description.getDisplayName())
    override def testStarted(description: Description): Unit =
      queue += TestStarted(description.getDisplayName())
    override def testSuiteFinished(description: Description): Unit =
      queue += TestSuiteFinished(description.getDisplayName())
  }

}
