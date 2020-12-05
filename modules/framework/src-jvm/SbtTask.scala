package weaver
package framework

import java.util.concurrent.atomic.AtomicBoolean

import sbt.testing.{ Event, EventHandler, Logger, Task, TaskDef }

private[framework] class SbtTask(
    val taskDef: TaskDef,
    isDone: AtomicBoolean,
    start: scala.concurrent.Promise[Unit],
    queue: java.util.concurrent.ConcurrentLinkedQueue[SuiteEvent])
    extends Task {

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = {
    start.success(())

    var finished: Boolean = false

    while (!finished && !isDone.get()) {
      val nextEvent = Option(queue.poll())

      nextEvent.foreach {
        case _: SuiteFinished | _: RunFinished => finished = true
        case TestFinished(outcome)             => eventHandler.handle(sbtEvent(outcome))
        case _                                 => ()
      }

      nextEvent.foreach(Reporter.log(loggers))
    }

    Array()
  }

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger],
      continuation: Array[Task] => Unit): Unit = {
    continuation(execute(eventHandler, loggers))
  }

  def tags(): Array[String] = Array()

  private def sbtEvent(outcome: TestOutcome): Event = SbtEvent(taskDef, outcome)
}
