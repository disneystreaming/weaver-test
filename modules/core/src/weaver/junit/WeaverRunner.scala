package weaver
package junit

import org.junit.runner.Description
import org.junit.runner.notification.RunNotifier
import weaver.TestStatus._

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

  def notifiying(notifier: RunNotifier): TestEvent => Unit = event => {
    event match {
      case TestStarted(_) =>
      // notifier.fireTestStarted(testDescriptions(name.name))
      case Ended(Success, outcome) =>
        notifier.fireTestStarted(testDescriptions(outcome.name))
        notifier.fireTestFinished(desc(outcome))
      case Ended(Cancelled, outcome) =>
        notifier.fireTestIgnored(desc(outcome))
      case Ended(Ignored, outcome) =>
        notifier.fireTestIgnored(desc(outcome))
      case Ended(Failure, outcome) =>
        notifier.fireTestStarted(testDescriptions(outcome.name))
        notifier.fireTestAssumptionFailed(failure(outcome))
      case Ended(weaver.TestStatus.Exception, outcome) =>
        notifier.fireTestStarted(testDescriptions(outcome.name))
        notifier.fireTestFailure(failure(outcome))
    }
  }

  private def desc(outcome: TestOutcome): Description =
    testDescriptions(outcome.name)

  private def failure(outcome: TestOutcome) = {

    val summary = outcome.cause.getOrElse {
      new Exception(outcome.formatted(TestOutcome.Verbose))
        with scala.util.control.NoStackTrace
    }

    new org.junit.runner.notification.Failure(desc(outcome), summary)

  }

  private object Ended {
    def unapply(event: TestEvent): Option[(TestStatus, TestOutcome)] =
      event match {
        case TestStarted(_)            => None
        case TestEnded(t: TestOutcome) => Some(t.status -> t)
      }
  }

}
