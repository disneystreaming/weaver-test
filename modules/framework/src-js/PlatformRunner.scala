package weaver
package framework

import sbt.testing.TaskDef
import sbt.testing.Task
import sbt.testing.{ EventHandler, Logger }

import scala.concurrent.duration._
import cats.data.Chain

import cats.syntax.all._
import cats.effect.concurrent.Ref
import cats.effect.ExitCase

trait PlatformRunner[F[_]] { self: sbt.testing.Runner =>
  protected val args: Array[String]
  protected val suiteLoader: SuiteLoader[F]
  protected val unsafeRun: UnsafeRun[F]

  import unsafeRun._

  override def deserializeTask(
      task: String,
      deserialize: String => sbt.testing.TaskDef): sbt.testing.Task = {
    val td = deserialize(task)

    val tsk = SbtTask(td,
                      suiteLoader(td).collect {
                        case suite: suiteLoader.SuiteRef => suite
                      }.get)

    tsk
  }

  override def serializeTask(
      task: sbt.testing.Task,
      serializer: sbt.testing.TaskDef => String): String = {
    serializer(task.taskDef())
  }

  override def done(): String = {
    "ITS OVER ITS DONE"
  }

  override def receiveMessage(msg: String): Option[String] = None

  override def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    val tasksAndSuites = taskDefs.toList.map { taskDef =>
      taskDef -> suiteLoader(taskDef)
    }.collect {
      case (taskDef, Some(suite: suiteLoader.SuiteRef)) => (taskDef, suite)
    }

    tasksAndSuites.map { case (td, ld) => SbtTask(td, ld) }.toArray
  }

  private case class SbtTask(td: TaskDef, loader: suiteLoader.SuiteRef)
      extends Task {
    override def tags(): Array[String] = Array()

    override def execute(
        eventHandler: EventHandler,
        loggers: Array[Logger]): Array[Task] = Array()

    override def execute(
        eventHandler: EventHandler,
        loggers: Array[Logger],
        continuation: Array[Task] => Unit): Unit = {

      val action = for {
        suite <- loader.suite
        suiteName = SuiteName(suite.name)
        _        <- effect.delay(Reporter.logSuiteStarted(loggers)(suiteName))
        outcomes <- Ref.of(Chain.empty[TestOutcome])
        
        outcomeHandle =
          (o: TestOutcome) =>
            effect.delay(
              Reporter.logTestFinished(loggers)(o)) *> outcomes.update(
              _.append(o))

        _ <- effect.guaranteeCase[Unit](suite.run(args.toList)(outcomeHandle)) {
          case ExitCase.Canceled => effect.unit
          case ExitCase.Completed =>
            val failedF =
              outcomes.get.map(_.filter(_.status.isFailed).map(suiteName -> _))
            failedF.flatMap {
              case c if c.isEmpty => effect.unit
              case failed =>
                effect.delay(Reporter.logRunFinished(loggers)(failed)).void
            }.void
          case ExitCase.Error(err) =>
            effect.unit
        }
      } yield ()

      unsafeRun.async(action.map(_ => continuation(Array())))
    }

    override def taskDef(): TaskDef = td

  }
}
