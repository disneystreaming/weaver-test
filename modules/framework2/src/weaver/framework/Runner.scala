package weaver
package framework

import sbt.testing._
import cats.syntax.all._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.syntax.all._

import java.util.concurrent.ConcurrentLinkedQueue
import cats.data.Chain
import cats.Parallel
import scala.reflect.ClassTag
import java.util.concurrent.atomic.AtomicBoolean

final class CatsIORunner(
    args: Array[String],
    remoteArgs: Array[String],
    classLoader: ClassLoader)(implicit CS: ContextShift[IO])
    extends AbstractRunner[IO, BaseIOSuite](args, remoteArgs, classLoader) {

  def runBackground(tasks: List[IOTask]): Unit =
    cancelToken = run(tasks).unsafeRunCancelable(_ => ())

  private var cancelToken: IO[Unit] = null

  def done(): String = {
    isDone.set(true)
    cancelToken.unsafeRunSync()
    System.lineSeparator()
  }

}

abstract class AbstractRunner[F[_], T <: EffectSuite[F]](
    val args: Array[String],
    val remoteArgs: Array[String],
    classLoader: ClassLoader)(
    implicit F: Concurrent[F],
    Par: Parallel[F],
    CS: ContextShift[F],
    T: ClassTag[T])
    extends Runner {

  // Flag meant to be raised if build-tool call `done`
  protected val isDone: AtomicBoolean = new AtomicBoolean(false)

  def runBackground(tasks: List[IOTask]): Unit

  def tasks(list: Array[TaskDef]): Array[Task] = {

    val (ioTasks, sbtTasks) = list.map[(IOTask, Task)] { taskDef =>
      val promise = scala.concurrent.Promise[Unit]()
      val queue   = new ConcurrentLinkedQueue[SuiteEvent]()

      val suite  = loadModuleAs[T](taskDef.fullyQualifiedName(), classLoader)
      val broker = new ConcurrentQueueEventBroker(queue)

      val ioTask =
        IOTask(suite,
               args.toList,
               Async.fromFuture(F.delay(promise.future)),
               broker)

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

  def run(tasks: List[IOTask]): F[Unit] = {
    import cats.syntax.all._

    for {
      ref <- Ref.of[F, Chain[(SuiteName, TestOutcome)]](Chain.empty)
      sem <- Semaphore[F](tasks.size.toLong)
      _   <- tasks.parTraverse(_.run(ref, sem, tasks.size.toLong))
    } yield ()
  }

  case class IOTask(
      suite: EffectSuite[F],
      args: List[String],
      start: F[Unit],
      broker: SuiteEventBroker) {
    def run(
        outcomes: Ref[F, Chain[(SuiteName, TestOutcome)]],
        semaphore: Semaphore[F],
        N: Long): F[Unit] = semaphore.withPermit {
      for {
        _ <- start // waiting for SBT to tell us to start
        _ <- broker.send(SuiteStarted(suite.name))
        _ <- suite
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
    def send(suiteEvent: SuiteEvent): F[Unit]
  }

  class ConcurrentQueueEventBroker(
      concurrentQueue: ConcurrentLinkedQueue[SuiteEvent])
      extends SuiteEventBroker {
    def send(suiteEvent: SuiteEvent): F[Unit] =
      Sync[F].delay(concurrentQueue.add(suiteEvent)).void
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

      while (!finished && !isDone.get()) {
        val nextEvent = Option(queue.poll())

        nextEvent.foreach {
          case _: SuiteFinished | _: RunFinished => finished = true
          case TestFinished(outcome)             => eventHandler.handle(sbtEvent(outcome))
          case _                                 => ()
        }

        nextEvent.foreach(Reporter.log(_, loggers))
      }

      Array()
    }

    def tags(): Array[String] = Array()

    private def sbtEvent(outcome: TestOutcome): Event = new Event {

      private val task = taskDef

      def fullyQualifiedName(): String =
        task.fullyQualifiedName()

      def throwable(): OptionalThrowable = outcome.cause match {
        case Some(value) => new OptionalThrowable(value)
        case None        => new OptionalThrowable()
      }

      def status(): Status =
        outcome.status match {
          case TestStatus.Exception => Status.Error
          case TestStatus.Failure   => Status.Failure
          case TestStatus.Success   => Status.Success
          case TestStatus.Ignored   => Status.Ignored
          case TestStatus.Cancelled => Status.Canceled
        }

      def selector(): Selector = {
        new TestSelector(outcome.name)
      }

      def fingerprint(): Fingerprint =
        task.fingerprint()

      def duration(): Long =
        outcome.duration.toMillis
    }
  }

  object Reporter {
    def log(event: SuiteEvent, loggers: Array[Logger]): Unit = event match {
      case SuiteStarted(name) =>
        loggers.foreach { logger =>
          logger.info(cyan(name))
        }
      case RunFinished(failed) =>
        if (failed.nonEmpty) {
          loggers.foreach(
            _.info(red("*************") + "FAILURES" + red("**************")))
        }
        failed.groupBy(_._1.name).foreach {
          case (suiteName, events) =>
            loggers.foreach(_.info(cyan(suiteName)))
            for ((_, event) <- events.iterator) {
              loggers.foreach(_.error(event.formatted(TestOutcome.Verbose)))
            }
            loggers.foreach(_.info(""))
        }
      case TestFinished(outcome) =>
        loggers.foreach { logger =>
          logger.info(outcome.formatted(TestOutcome.Summary))
        }

      case _: SuiteFinished => ()
    }
  }

  object SbtTask {
    def apply(
        taskDef: TaskDef,
        start: scala.concurrent.Promise[Unit],
        queue: java.util.concurrent.ConcurrentLinkedQueue[SuiteEvent]): Task =
      new SbtTask(taskDef, start, queue)
  }

}

final case class SuiteName(name: String) extends AnyVal

sealed trait SuiteEvent
case class SuiteStarted(name: String)         extends SuiteEvent
case class TestFinished(outcome: TestOutcome) extends SuiteEvent
case class SuiteFinished(name: String)        extends SuiteEvent
case class RunFinished(failedOutcomes: Chain[(SuiteName, TestOutcome)])
    extends SuiteEvent
