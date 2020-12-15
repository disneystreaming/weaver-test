package weaver
package framework

import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

import cats.data.Chain

import sbt.testing.{ Event, EventHandler, Logger, Task, TaskDef }

private[framework] class SbtTask(
    val taskDef: TaskDef,
    isDone: AtomicBoolean,
    stillRunning: AtomicInteger,
    start: scala.concurrent.Promise[Unit],
    queue: java.util.concurrent.ConcurrentLinkedQueue[SuiteEvent],
    loggerPermit: java.util.concurrent.Semaphore,
    readFailed: () => Chain[(SuiteName, TestOutcome)]
) extends Task {

  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = {
    val log = Reporter.log(loggers)(_)

    start.trySuccess(())

    var finished: Boolean = false

    loggerPermit.acquire()
    try {
      while (!finished && !isDone.get()) {
        val nextEvent = Option(queue.poll())

        nextEvent.foreach {
          case s @ SuiteStarted(_) => log(s)
          case SuiteFinished(_) =>
            finished = true
            if (stillRunning.decrementAndGet == 0) {
              log(RunFinished(readFailed()))
            }
          case t @ TestFinished(outcome) =>
            eventHandler.handle(sbtEvent(outcome))
            log(t)
        }
      }
    } finally {
      loggerPermit.release()
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
