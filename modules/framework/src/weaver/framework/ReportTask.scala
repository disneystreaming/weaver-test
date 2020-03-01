package weaver
package framework

import weaver.TestOutcome

import cats.effect.IO
import cats.data.Chain
import cats.implicits._

import sbt.testing.{ TaskDef, Task => BaseTask, Logger => BaseLogger, _ }

import scala.concurrent.{ Await, Promise }
import scala.concurrent.duration.Duration

final class ReportTask(
    withLog: (Chain[(String, TestOutcome)] => IO[Unit]) => IO[Unit])
    extends WeaverTask { self =>

  def tags(): Array[String] = Array.empty

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger],
      continuation: Array[BaseTask] => Unit): Unit = {

    discard[EventHandler](eventHandler)
    withLog { log =>
      IO {
        if (log.nonEmpty) {
          loggers.foreach(
            _.info(red("*************") + "FAILURES" + red("**************")))
        }
        log.groupBy(_._1).foreach {
          case (suiteName, events) =>
            loggers.foreach(_.info(cyan(suiteName)))
            for ((_, event) <- events.iterator) {
              loggers.foreach(_.error(event.formatted))
            }
            loggers.foreach(_.info(""))
        }
      }
    }
  }.unsafeRunAsync {
    case Right(_) => continuation(Array.empty)
    case Left(e)  => e.printStackTrace(); continuation(Array.empty)
  }

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger]): Array[BaseTask] = {

    val p = Promise[Array[BaseTask]]()
    execute(eventHandler, loggers, tasks => p.success(tasks))
    Await.result(p.future, Duration.Inf)
  }

  override def taskDef(): TaskDef = {
    new TaskDef("weaver.framework.ReportTask",
                new Fingerprint {},
                false,
                Array())
  }

}
