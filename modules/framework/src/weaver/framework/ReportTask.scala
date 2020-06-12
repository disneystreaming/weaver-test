package weaver
package framework

import weaver.TestOutcome

import cats.effect.IO
import cats.data.Chain
import cats.implicits._

import sbt.testing.{ TaskDef, Task => BaseTask, Logger => BaseLogger, _ }

final class ReportTask(
    processLogs: (Chain[(String, TestOutcome)] => IO[Unit]) => IO[Unit])
    extends WeaverTask { self =>

  def tags(): Array[String] = Array.empty

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger],
      continuation: Array[BaseTask] => Unit): Unit = {
    executeWrapper(eventHandler, loggers)
      .map(continuation)
      .unsafeRunAsyncAndForget()
  }

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger]): Array[BaseTask] = {
    executeWrapper(eventHandler, loggers).unsafeRunSync()
  }

  override def taskDef(): TaskDef = {
    new TaskDef("weaver.framework.ReportTask",
                new Fingerprint {},
                false,
                Array())
  }

  private def executeWrapper(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger]): IO[Array[BaseTask]] = {
    discard[EventHandler](eventHandler)
    processLogs(ReportTask.report(loggers)).attempt.map {
      case Right(_) => Array.empty[BaseTask]
      case Left(e)  => e.printStackTrace(); Array.empty[BaseTask]
    }
  }
}

object ReportTask {

  def report(loggers: Array[BaseLogger])(
      logs: Chain[(String, TestOutcome)]): IO[Unit] = IO {
    if (logs.nonEmpty) {
      loggers.foreach(
        _.info(red("*************") + "FAILURES" + red("**************")))
    }
    logs.groupBy(_._1).foreach {
      case (suiteName, events) =>
        loggers.foreach(_.info(cyan(suiteName)))
        for ((_, event) <- events.iterator) {
          loggers.foreach(_.error(event.formatted(TestOutcome.Verbose)))
        }
        loggers.foreach(_.info(""))
    }
  }

}
