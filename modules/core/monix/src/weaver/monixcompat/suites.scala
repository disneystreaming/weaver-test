package weaver
package monixcompat

import cats.effect.Resource

import monix.eval.Task
import monix.execution.Scheduler

trait BaseTaskSuite extends EffectSuite[Task]

abstract class PureTaskSuite
    extends EffectSuite[Task]
    with BaseTaskSuite
    with Expectations.Helpers {

  def pureTest(name: String)(run: => Expectations): Task[TestOutcome] =
    Test[Task](name, Task(run))
  def simpleTest(name: String)(run: Task[Expectations]): Task[TestOutcome] =
    Test[Task](name, run)
  def loggedTest(name: String)(
      run: Log[Task] => Task[Expectations]): Task[TestOutcome] =
    Test[Task](name, run)

}

abstract class MutableTaskSuite
    extends RunnableSuite[Task]
    with BaseTaskSuite
    with Expectations.Helpers {

  implicit protected def effectCompat = MonixUnsafeRun

  final implicit protected def scheduler: Scheduler = effectCompat.scheduler
}

trait SimpleMutableTaskSuite extends MutableTaskSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure[Task, Unit](())
}

trait FunTaskSuite
    extends FunSuiteAux[Task]
    with BaseTaskSuite
    with Expectations.Helpers {
  implicit protected def effectCompat = MonixUnsafeRun

  final implicit protected def scheduler: Scheduler = effectCompat.scheduler
}
