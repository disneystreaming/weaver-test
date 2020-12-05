package weaver
package monixcompat

import cats.effect.Resource

import monix.eval.Task

trait BaseTaskSuite extends RunnableSuite[Task] {
  val unsafeRun: UnsafeRun[Task]      = MonixUnsafeRun
  implicit protected def contextShift = unsafeRun.contextShift
  implicit protected def timer        = unsafeRun.timer
}

trait PureTaskSuite
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

trait MutableTaskSuite
    extends MutableFSuite[Task]
    with BaseTaskSuite
    with Expectations.Helpers

trait SimpleMutableTaskSuite extends MutableTaskSuite {
  type Res = Unit
  def sharedResource: Resource[Task, Unit] = Resource.pure[Task, Unit](())
}
