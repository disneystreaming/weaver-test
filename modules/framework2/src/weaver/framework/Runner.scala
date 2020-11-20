package weaver
package framework

import sbt.testing._
import cats.syntax.all._
import cats.effect.IO
import java.util.concurrent.ConcurrentLinkedQueue
import scala.concurrent.ExecutionContext
import cats.effect.concurrent.Ref
import cats.data.Chain
import cats.effect.concurrent.Semaphore

final class CatsIORunner(
    val args: Array[String],
    val remoteArgs: Array[String],
    classLoader: ClassLoader)
    extends Runner {

  implicit val cs    = IO.contextShift(ExecutionContext.global)
  implicit val timer = IO.timer(ExecutionContext.global)

  def tasks(list: Array[TaskDef]): Array[Task] = {
    val (ioTasks, sbtTasks) = list.map[(IOTask, Task)] { taskDef =>
      val promise = scala.concurrent.Promise[Unit]()
      val queue   = new ConcurrentLinkedQueue[SuiteEvent]()

      val suite  = suiteFromModule(taskDef.fullyQualifiedName(), classLoader)
      val broker = new ConcurrentQueueEventBroker(queue)

      val ioTask =
        IOTask(suite, args.toList, IO.fromFuture(IO(promise.future)), broker)

      val sbtTask = SbtTask(taskDef, promise, queue)
      (ioTask, sbtTask)
    }.unzip

    runBackground(ioTasks.toList)

    sbtTasks
  }

  def serializeTask(task: Task, serializer: TaskDef => String): String = ???

  def deserializeTask(
      task: String,
      deserializer: String => TaskDef): Task = ???

  def done(): String = ???

  def runBackground(tasks: List[IOTask]): Unit =
    run(tasks).unsafeRunAsyncAndForget()

  def run(tasks: List[IOTask]): IO[Unit] = {
    import cats.syntax.all._

    for {
      ref <- Ref.of[IO, Chain[(SuiteName, TestOutcome)]](Chain.empty)
      sem <- Semaphore[IO](tasks.size.toLong)
      _   <- tasks.parTraverse(_.run(ref, sem, tasks.size.toLong))
    } yield ()
  }

}

case class IOTask(
    suite: BaseIOSuite,
    args: List[String],
    start: IO[Unit],
    broker: SuiteEventBroker) {
  def run(
      outcomes: Ref[IO, Chain[(SuiteName, TestOutcome)]],
      semaphore: Semaphore[IO],
      N: Long): IO[Unit] = semaphore.withPermit {
    for {
      _ <- start // waiting for SBT to tell us to start
      _ <- broker.send(SuiteStarted(suite.name))
      _ <-
        suite
          .run(args) { testOutcome =>
            outcomes.update(
              _.append(SuiteName(suite.name) -> testOutcome)).whenA(
              testOutcome.status.isFailed) *>
              broker.send(TestFinished(testOutcome))
          } // todo : error handling
    } yield ()
  }.guarantee {
    semaphore.tryAcquireN(N).flatMap {
      case true  => outcomes.get.flatMap(o => broker.send(RunFinished(o)))
      case false => broker.send(SuiteFinished(suite.name))
    }
  }
}

trait SuiteEventBroker {
  def send(suiteEvent: SuiteEvent): IO[Unit]
}

class ConcurrentQueueEventBroker(
    concurrentQueue: ConcurrentLinkedQueue[SuiteEvent])
    extends SuiteEventBroker {
  def send(suiteEvent: SuiteEvent): IO[Unit] =
    IO(concurrentQueue.add(suiteEvent)).void
}

class SbtTask(
    val taskDef: TaskDef,
    start: scala.concurrent.Promise[Unit],
    queue: java.util.concurrent.ConcurrentLinkedQueue[SuiteEvent])
    extends Task {
  def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = {
    start.success(())
    var finished: Boolean = false
    while (!finished) {
      val nextEvent = Option(queue.poll())
      nextEvent.foreach {
        case SuiteStarted(name) =>
          loggers.foreach { logger =>
            logger.info(name)
          }
        case RunFinished(failed) =>
          // log aggregated failures
          finished = true
          loggers.foreach { logger =>
            failed.toList.foreach { o =>
              logger.error(s"FAILED: ${o._1}:\n${o._2.name}")
            }
          }
        case SuiteFinished(_) =>
          finished = true
        case TestFinished(outcome) =>
          loggers.foreach { logger => logger.info(outcome.name) }
        // log test and send event
      }
    }
    Array()
  }

  def tags(): Array[String] = Array()
}

object SbtTask {
  def apply(
      taskDef: TaskDef,
      start: scala.concurrent.Promise[Unit],
      queue: java.util.concurrent.ConcurrentLinkedQueue[SuiteEvent]): Task =
    new SbtTask(taskDef, start, queue)
}

final case class SuiteName(name: String) extends AnyVal

sealed trait SuiteEvent
case class SuiteStarted(name: String)         extends SuiteEvent
case class TestFinished(outcome: TestOutcome) extends SuiteEvent
case class SuiteFinished(name: String)        extends SuiteEvent
case class RunFinished(failedOutcomes: Chain[(SuiteName, TestOutcome)])
    extends SuiteEvent
