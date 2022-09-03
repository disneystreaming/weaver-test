package weaver
package junit

import weaver.TestStatus._
import weaver.internals.Reflection

import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier

class WeaverRunner(cls: Class[_], dummy: Boolean)
    extends org.junit.runner.Runner {

  type F[A] = Any

  def this(cls: Class[_]) = this(cls, true)

  lazy val suite: RunnableSuite[F] = {
    Reflection.loadRunnableSuite(cls.getName(), getClass().getClassLoader())
  }

  def testDescriptions: Map[String, Description] = {
    suite.plan.map(name =>
      name.name -> Description.createTestDescription(cls, name.name)).toMap
  }

  def getDescription(): Description = {
    val desc = Description.createSuiteDescription(cls)
    testDescriptions.values.foreach(desc.addChild)
    desc
  }

  def run(notifier: RunNotifier): Unit = {
    val desc = getDescription()

    notifier.fireTestSuiteStarted(desc)
    notifyIgnored(notifier)
    suite.runUnsafe(List.empty)(notifiying(notifier))
    notifier.fireTestSuiteFinished(desc)
  }

  def notifiying(notifier: RunNotifier): TestOutcome => Unit = outcome => {
    val description = desc(outcome)
    outcome.status match {
      case Success =>
        notifier.fireTestStarted(description)
        notifier.fireTestFinished(description)
      case Failure =>
        notifier.fireTestStarted(description)
        notifier.fireTestFailure(assertionFailed(outcome))
        notifier.fireTestFinished(description)
      case weaver.TestStatus.Exception =>
        notifier.fireTestStarted(description)
        notifier.fireTestFailure(failure(outcome))
      case Cancelled =>
        notifier.fireTestIgnored(description)
      case Ignored =>
        notifier.fireTestIgnored(description)
    }
  }

  private def notifyIgnored(notifier: RunNotifier): Unit = {
    val (taggedIgnored, rest) = suite.plan.partition(_.tags(TestName.Tags.ignore))
    val (only, ignored) = rest.partition(_.tags(TestName.Tags.only))
    val toNotifyIgnored = if (only.nonEmpty) {
      taggedIgnored ++ ignored
    } else {
      taggedIgnored
    }
    toNotifyIgnored
      .map(_.name)
      .map(testDescriptions)
      .foreach(notifier.fireTestIgnored)
  }

  private def desc(outcome: TestOutcome): Description =
    testDescriptions(outcome.name)

  private def failure(outcome: TestOutcome) = {
    val summary = outcome.cause.getOrElse {
      new Exception(
        Colours.removeASCIIColors(outcome.formatted(TestOutcome.Verbose)))
        with scala.util.control.NoStackTrace
    }

    new org.junit.runner.notification.Failure(desc(outcome), summary)
  }

  private def assertionFailed(outcome: TestOutcome) = {
    val summary =
      new org.junit.AssumptionViolatedException(
        Colours.removeASCIIColors(outcome.formatted(TestOutcome.Verbose)))
        with scala.util.control.NoStackTrace

    new org.junit.runner.notification.Failure(desc(outcome), summary)
  }

}
