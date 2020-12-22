package weaver
package framework

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.JSON

import cats.data.Chain
import cats.effect.Sync
import cats.syntax.all._

import sbt.testing.{ EventHandler, Logger, Task, TaskDef }

import CECompat.Ref

trait RunnerCompat[F[_]] { self: sbt.testing.Runner =>
  protected val args: Array[String]
  protected val suiteLoader: SuiteLoader[F]
  protected val unsafeRun: UnsafeRun[F]
  protected val channel: Option[String => Unit]

  import unsafeRun._

  private[weaver] val failedTests = ListBuffer.empty[(SuiteName, TestOutcome)]

  def reportDone(out: TestOutcomeJS): Unit = {
    val serialised = JSON.stringify(out)
    channel match {
      case Some(send) => send(serialised)
      case None       => failedTests.append(TestOutcomeJS.rehydrate(out))
    }
  }

  def reportDoneF(out: TestOutcomeJS): F[Unit] = Sync[F].delay(reportDone(out))

  override def deserializeTask(
      task: String,
      deserialize: String => sbt.testing.TaskDef): sbt.testing.Task = {
    val taskDef = deserialize(task)

    val suiteRefs = suiteLoader(taskDef).collect {
      case suite: suiteLoader.SuiteRef => suite
    }

    SbtTask(taskDef, suiteRefs)
  }

  override def serializeTask(
      task: sbt.testing.Task,
      serializer: sbt.testing.TaskDef => String): String = {
    serializer(task.taskDef())
  }

  override def done(): String = {
    val sb = new StringBuilder

    val s = { (str: String) =>
      val _ = sb.append(str + TaskCompat.lineSeparator)
    }

    Reporter.runFinished(s, s)(Chain(failedTests.toSeq: _*))

    sb.result()
  }

  override def receiveMessage(msg: String): Option[String] = {
    val outcome = JSON.parse(msg).asInstanceOf[TestOutcomeJS]
    reportDone(outcome)
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

      def reportTest(outcome: TestOutcome) =
        effect.delay(eventHandler.handle(SbtEvent(td, outcome)))

      def runSuite(
          fqn: String,
          suite: EffectSuite[F],
          outcomes: Ref[F, Chain[TestOutcome]]): F[Unit] = for {
        _ <- effect.delay(Reporter.logSuiteStarted(loggers)(SuiteName(fqn)))
        _ <- suite.run(args.toList) { outcome =>
          effect.delay(Reporter.logTestFinished(loggers)(outcome))
            .productR(reportTest(outcome))
            .productR(outcomes.update(_.append(outcome)))
        }
      } yield ()

      def finaliseCompleted(outcomes: Ref[F, Chain[TestOutcome]]): F[Unit] = {
        val failedF = outcomes.get.map(
          _.filter(_.status.isFailed).map(SuiteName(fqn) -> _))

        failedF.flatMap {
          case c if c.isEmpty => effect.unit
          case failed => {
            val ots: Chain[TestOutcomeJS] =
              failed.map { case (SuiteName(name), to) =>
                TestOutcomeJS.from(name)(to)
              }

            ots.traverse(reportDoneF).void
          }
        }
      }

      def finaliseError(outcomes: Ref[
        F,
        Chain[TestOutcome]]): Throwable => F[Unit] = { error =>
        val outcome =
          TestOutcome("Unexpected failure",
                      0.seconds,
                      Result.from(error),
                      Chain.empty)
        reportTest(outcome)
          .productR(reportDoneF(TestOutcomeJS.from(fqn)(outcome)))
      }

      val action = loader match {
        case None => effect.unit
        case Some(loader) => for {
            outcomes <- Ref.of(Chain.empty[TestOutcome])
            _ <- CECompat.guaranteeCase(loader.suite
              .flatMap(runSuite(fqn, _, outcomes)))(
              cancelled = effect.unit,
              completed = finaliseCompleted(outcomes),
              errored = finaliseError(outcomes)
            )
          } yield ()
      }

      unsafeRun.async(action.attempt.map { exc =>
        continuation(Array())
      })
    }

    override def taskDef(): TaskDef = td

  }

}

class TestOutcomeJS(
    val suiteName: String,
    val testName: String,
    val durationMs: Double,
    val verboseFormatting: String
) extends js.Object {}

object TestOutcomeJS {
  def from(suiteName: String)(outcome: TestOutcome): TestOutcomeJS = {
    TestOutcomeJS(
      suiteName,
      outcome.name,
      outcome.duration.toMillis.toDouble,
      outcome.formatted(TestOutcome.Verbose))
  }

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
    SuiteName(t.suiteName) -> DecodedOutcome(
      t.testName,
      t.durationMs.millis,
      t.verboseFormatting
    )
  }

  private case class DecodedOutcome(
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
}
