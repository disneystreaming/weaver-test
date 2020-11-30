package weaver
package framework

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.scalajs.js.JSON

import cats.data.Chain
import cats.effect.ExitCase
import cats.effect.concurrent.Ref
import cats.syntax.all._

import sbt.testing.{ EventHandler, Logger, Task, TaskDef }

trait PlatformRunner[F[_]] { self: sbt.testing.Runner =>
  protected val args: Array[String]
  protected val suiteLoader: SuiteLoader[F]
  protected val unsafeRun: UnsafeRun[F]
  protected val channel: Option[String => Unit]

  import unsafeRun._

  private[weaver] val failedTests = ListBuffer.empty[(SuiteName, TestOutcome)]

  def reportDone(out: TestOutcomeJS) = {
    val serialised = JSON.stringify(out)
    channel match {
      case Some(send) => send(serialised)
      case None       => failedTests.append(TestOutcomeJS.rehydrate(out))
    }
  }

  override def deserializeTask(
      task: String,
      deserialize: String => sbt.testing.TaskDef): sbt.testing.Task = {
    val td = deserialize(task)

    SbtTask(td,
            suiteLoader(td).collect {
              case suite: suiteLoader.SuiteRef => suite
            })

  }

  override def serializeTask(
      task: sbt.testing.Task,
      serializer: sbt.testing.TaskDef => String): String = {
    serializer(task.taskDef())
  }

  override def done(): String = {
    val sb = new StringBuilder

    val s: String => Unit =
      str => { val _ = sb.append(str + TaskCompat.lineSeparator) }

    Reporter.runFinished(s, s)(Chain(failedTests.toSeq: _*))

    sb.result()
  }

  override def receiveMessage(msg: String): Option[String] = {
    val deser = JSON.parse(msg).asInstanceOf[TestOutcomeJS]
    reportDone(deser);
    None
  }

  override def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    val tasksAndSuites = taskDefs.toList.map { taskDef =>
      taskDef -> suiteLoader(taskDef)
    }.collect {
      case (taskDef, Some(suite: suiteLoader.SuiteRef)) => (taskDef, suite)
    }

    tasksAndSuites.map { case (td, ld) => SbtTask(td, Some(ld)) }.toArray
  }

  private case class SbtTask(td: TaskDef, loader: Option[suiteLoader.SuiteRef])
      extends Task {
    override def tags(): Array[String] = Array()

    override def execute(
        eventHandler: EventHandler,
        loggers: Array[Logger]): Array[Task] = Array()

    override def execute(
        eventHandler: EventHandler,
        loggers: Array[Logger],
        continuation: Array[Task] => Unit): Unit = {

      val fqn = taskDef().fullyQualifiedName()

      def reportTestToSbt(outcome: TestOutcome) =
        effect.delay(eventHandler.handle(SbtEvent(td, outcome)))

      loader match {
        case None => continuation(Array())
        case Some(loader) =>
          def runSuite(outcomes: Ref[F, Chain[TestOutcome]]) = for {
            suite <- loader.suite
            _     <- effect.delay(Reporter.logSuiteStarted(loggers)(SuiteName(fqn)))
            _ <- suite.run(args.toList) { outcome =>
              effect.delay(Reporter.logTestFinished(loggers)(outcome)) *>
                reportTestToSbt(outcome) *>
                outcomes.update(_.append(outcome))
            }
          } yield ()

          val action = for {
            outcomes <- Ref.of(Chain.empty[TestOutcome])
            run = runSuite(outcomes)
            _ <-
              effect.guaranteeCase[Unit](run) {
                case ExitCase.Canceled =>
                  effect.unit
                case ExitCase.Completed =>
                  val failedF =
                    outcomes.get.map(
                      _.filter(_.status.isFailed).map(SuiteName(fqn) -> _))

                  failedF.flatMap {
                    case c if c.isEmpty => effect.unit
                    case failed => {
                      val ots: Chain[TestOutcomeJS] =
                        failed.map { case (SuiteName(name), to) =>
                          TestOutcomeJS(name,
                                        to.name,
                                        to.duration.toMillis.toDouble,
                                        to.formatted(TestOutcome.Verbose))
                        }

                      ots.traverse(o => effect.delay(reportDone(o))).void
                    }
                  }.void
                case ExitCase.Error(error) =>
                  val outcome =
                    TestOutcome("Unexpected failure",
                                0.seconds,
                                Result.from(error),
                                Chain.empty)

                  reportTestToSbt(outcome) *> 
                  effect.delay(reportDone(TestOutcomeJS(
                    fqn,
                    outcome.name,
                    outcome.duration.toMillis.toDouble,
                    outcome.formatted(TestOutcome.Verbose)))).void
              }
          } yield ()

          unsafeRun.async(action.attempt.map { exc =>
            continuation(Array())
          })
      }
    }

    override def taskDef(): TaskDef = td

  }

}

import scala.scalajs.js

class TestOutcomeJS(
    val suiteName: String,
    val testName: String,
    val durationMs: Double,
    val verboseFormatting: String
) extends js.Object {}

case class MyOutcome(
    testName: String,
    dur: FiniteDuration,
    verboseFormatting: String)
    extends TestOutcome {
  def name: String                              = testName
  def duration: FiniteDuration                  = dur
  def status: TestStatus                        = TestStatus.Failure
  def log: Chain[Log.Entry]                     = Chain.empty
  def formatted(mode: TestOutcome.Mode): String = verboseFormatting
  def cause: Option[Throwable]                  = None
}

object TestOutcomeJS {
  def apply(
      suiteName: String,
      testName: String,
      durationMs: Double,
      verboseFormatting: String): TestOutcomeJS =
    js.Dynamic.literal(
      suiteName = suiteName,
      testName = testName,
      durationMs = durationMs,
      verboseFormatting = verboseFormatting).asInstanceOf[TestOutcomeJS]

  def rehydrate(t: TestOutcomeJS): (SuiteName, TestOutcome) = {
    SuiteName(t.suiteName) -> MyOutcome(
      t.testName,
      t.durationMs.millis,
      t.verboseFormatting
    )
  }
}
