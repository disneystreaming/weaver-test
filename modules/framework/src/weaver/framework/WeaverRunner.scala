package weaver
package framework

import sbt.testing._
import cats.data.Chain
import cats.syntax.all._
import cats.effect._
import cats.effect.concurrent._
import cats.effect.syntax.all._

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import scala.concurrent.ExecutionContext

class WeaverRunner[F[_]](
    val args: Array[String],
    val remoteArgs: Array[String],
    suiteLoader: SuiteLoader[F],
    unsafeRun: UnsafeRun[F]
) extends Runner {

  import unsafeRun._

  private var cancelToken: F[Unit] = unsafeRun.void

  override def done(): String = {
    isDone.set(true)
    unsafeRun.sync(cancelToken)
    System.lineSeparator()

  }

  // Required on js
  def receiveMessage(msg: String): Option[String] = None

  // Flag meant to be raised if build-tool call `done`
  protected val isDone: AtomicBoolean = new AtomicBoolean(false)

  private def runBackground(
      globalResources: List[GlobalResourcesInit[F]],
      tasks: List[IOTask]): Unit = {
    cancelToken = unsafeRun.background(run(globalResources, tasks))
  }

  def tasks(taskDefs: Array[TaskDef]): Array[Task] = {

    val tasksAndSuites = taskDefs.toList.map { taskDef =>
      taskDef -> suiteLoader(taskDef)
    }.collect { case (taskDef, Some(suite)) => (taskDef, suite) }

    def makeTasks(
        taskDef: TaskDef,
        mkSuite: GlobalResources.Read[F] => EffectSuite[F]): (IOTask, Task) = {
      val promise = scala.concurrent.Promise[Unit]()
      val queue   = new ConcurrentLinkedQueue[SuiteEvent]()
      val broker  = new ConcurrentQueueEventBroker(queue)
      val startingBlock = Async.fromFuture {
        Sync[F].delay(promise.future.map(_ => ())(ExecutionContext.global))
      }

      val ioTask =
        IOTask(
          taskDef.fullyQualifiedName(),
          mkSuite,
          args.toList,
          startingBlock,
          broker)

      val sbtTask = SbtTask(taskDef, promise, queue)
      (ioTask, sbtTask)
    }

    val (ioTasks, sbtTasks) = tasksAndSuites.collect[(IOTask, Task)] {
      case (taskDef, suiteLoader.ModuleSuite(suite)) =>
        makeTasks(taskDef, _ => suite)
      case (taskDef, suiteLoader.ResourcesSharingSuite(mkSuite)) =>
        makeTasks(taskDef, mkSuite)
    }.unzip

    val globalResources = tasksAndSuites.collect {
      case (_, suiteLoader.GlobalResourcesRef(init)) => init
    }.toList

    runBackground(globalResources, ioTasks.toList)

    sbtTasks.toArray
  }

  def serializeTask(task: Task, serializer: TaskDef => String): String = ???

  def deserializeTask(
      task: String,
      deserializer: String => TaskDef): Task = ???

  private def run(
      globalResources: List[GlobalResourcesInit[F]],
      tasks: List[IOTask]): F[Unit] = {
    import cats.syntax.all._
    resourceMap(globalResources).use { read =>
      for {
        ref <- Ref.of[F, Chain[(SuiteName, TestOutcome)]](Chain.empty)
        sem <- Semaphore[F](0L)
        _   <- tasks.parTraverse(_.run(read, ref, sem, tasks.size.toLong))
      } yield ()
    }
  }

  private def resourceMap(
      globalResources: List[GlobalResourcesInit[F]]
  ): Resource[F, GlobalResources.Read[F]] =
    Resource.liftF(GlobalResources.createMap[F]).flatTap { map =>
      globalResources.traverse(_.sharedResources(map)).void
    }

  private case class IOTask(
      fqn: String,
      mkSuite: GlobalResources.Read[F] => EffectSuite[F],
      args: List[String],
      start: F[Unit],
      broker: SuiteEventBroker) {
    def run(
        globalResources: GlobalResources.Read[F],
        outcomes: Ref[F, Chain[(SuiteName, TestOutcome)]],
        semaphore: Semaphore[F],
        N: Long): F[Unit] = {
      val suite = mkSuite(globalResources)
      val runSuite = for {
        _ <- start // waiting for SBT to tell us to start
        _ <- broker.send(SuiteStarted(suite.name))
        _ <- suite.run(args) { testOutcome =>
          outcomes
            .update(_.append(SuiteName(suite.name) -> testOutcome))
            .whenA(testOutcome.status.isFailed)
            .productR(broker.send(TestFinished(testOutcome)))
        }
      } yield ()
      runSuite.guaranteeCase { exitCase =>
        semaphore.release *> semaphore.tryAcquireN(N).flatMap {
          case true  => outcomes.get.flatMap(o => broker.send(RunFinished(o)))
          case false => broker.send(SuiteFinished(fqn))
        }
      }
    }
  }

  trait SuiteEventBroker {
    def send(suiteEvent: SuiteEvent): F[Unit]
  }

  class ConcurrentQueueEventBroker(
      concurrentQueue: ConcurrentLinkedQueue[SuiteEvent])
      extends SuiteEventBroker {
    def send(suiteEvent: SuiteEvent): F[Unit] = {
      Sync[F].delay(concurrentQueue.add(suiteEvent)).void
    }
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
