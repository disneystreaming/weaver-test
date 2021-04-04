package weaver
package junit

import weaver.TestStatus._

import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier

class WeaverRunner(cls: Class[_], dummy: Boolean)
    extends org.junit.runner.Runner {

  def this(cls: Class[_]) = this(cls, true)

  lazy val suite: RunnableSuite[Any] = {
    val mirror =
      scala.reflect.runtime.universe.runtimeMirror(getClass().getClassLoader())
    val module = mirror.staticModule(cls.getName())
    val obj    = mirror.reflectModule(module)
    obj.instance.asInstanceOf[RunnableSuite[Any]]
  }

  lazy val testDescriptions: Map[String, Description] = {
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
