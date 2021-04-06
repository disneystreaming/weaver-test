package weaver
package junit

import cats.effect._

import org.junit.runner.notification.RunListener
import org.junit.runner.Description
import org.junit.runner.notification.Failure
import org.junit.runner.notification.RunNotifier
import cats.Show

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
            s.contains("JUnitRunnerTests.scala")))
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

  def run(
      blocker: BlockerCompat[IO],
      suite: SimpleIOSuite): IO[List[Notification]] = for {
    runner   <- IO(new WeaverRunner(suite.getClass()))
    queue    <- IO(scala.collection.mutable.Queue.empty[Notification])
    notifier <- IO(new RunNotifier())
    _        <- IO(notifier.addListener(new NotificationListener(queue)))
    _        <- blocker.block(runner.run(notifier))
  } yield queue.toList

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

object Meta {

  object MySuite extends SimpleIOSuite {

    override def maxParallelism: Int = 1

    pureTest("success") {
      success
    }

    pureTest("failure") {
      failure("oops")
    }

    test("ignore") {
      ignore("just because")
    }

  }

  object Only extends SimpleIOSuite {

    override def maxParallelism: Int = 1

    pureTest("only".only) {
      success
    }

    pureTest("not only") {
      failure("foo")
    }

  }

}
