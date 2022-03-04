package weaver
package framework

import java.io.PrintStream
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.{ AtomicBoolean, AtomicInteger }

import scala.concurrent.duration._
import scala.concurrent.{ ExecutionContext, Promise }

import cats.data.Chain
import cats.effect.{ Sync, _ }
import cats.syntax.all._

import sbt.testing.{ Task, TaskDef }

import CECompat.Ref
import CECompat.Semaphore

import java.util.ServiceLoader
import scala.collection.mutable.ListBuffer

trait RunnerCompat[F[_]] { self: sbt.testing.Runner =>

  protected val suiteLoader: SuiteLoader[F]
  protected val unsafeRun: UnsafeRun[F]
  protected val errorStream: PrintStream
  import unsafeRun._

  private type MakeSuite = GlobalResourceF.Read[F] => F[EffectSuite[F]]

  private var cancelToken: Option[unsafeRun.CancelToken] = None

  @volatile private var failedOutcomes: Chain[(SuiteName, TestOutcome)] =
    Chain.empty

  override def done(): String = {
    isDone.set(true)
    cancelToken.foreach(unsafeRun.cancel)
    ""
  }

  // Required on js
  def receiveMessage(msg: String): Option[String] = None

  // Flag meant to be raised if build-tool call `done`
  protected val isDone: AtomicBoolean = new AtomicBoolean(false)

  private def runBackground(
      globalResources: List[GlobalResourceF[F]],
      waitForResourcesShutdown: java.util.concurrent.Semaphore,
      tasks: List[IOTask],
      gate: Promise[Unit]): Unit = {
    cancelToken = Some(unsafeRun.background(run(globalResources,
                                                waitForResourcesShutdown,
                                                tasks,
                                                gate)))
  }

  def tasks(taskDefs: Array[TaskDef]): Array[Task] = {
    val stillRunning             = new AtomicInteger(0)
    val waitForResourcesShutdown = new java.util.concurrent.Semaphore(0)

    val tasksAndSuites = taskDefs.toList.map { taskDef =>
      taskDef -> suiteLoader(taskDef)
    }.collect { case (taskDef, Some(suite)) => (taskDef, suite) }

    def makeTasks(
        taskDef: TaskDef,
        mkSuite: MakeSuite): (IOTask, Task) = {
      val promise = scala.concurrent.Promise[Unit]()
      if (args().contains("--quickstart") || args().contains("-qs"))
        promise.success(())

      // 1 permit semaphore protecting against build tools not
      // dispatching logs through a single logger at a time.
      val loggerPermit = new java.util.concurrent.Semaphore(1, true)

      val queue  = new ConcurrentLinkedQueue[SuiteEvent]()
      val broker = new ConcurrentQueueEventBroker(queue)
      val startingBlock = unsafeRun.fromFuture {
        promise.future.map(_ => ())(ExecutionContext.global)
      }

      val ioTask =
        IOTask(
          taskDef.fullyQualifiedName(),
          mkSuite,
          args.toList,
          startingBlock,
          broker)

      val sbtTask =
        new SbtTask(taskDef,
                    isDone,
                    stillRunning,
                    waitForResourcesShutdown,
                    promise,
                    queue,
                    loggerPermit,
                    () => failedOutcomes)
      (ioTask, sbtTask)
    }

    val (ioTasks, sbtTasks) = tasksAndSuites.collect {
      case (taskDef, suiteLoader.SuiteRef(mkSuite)) =>
        makeTasks(taskDef, _ => mkSuite)
      case (taskDef, suiteLoader.ResourcesSharingSuiteRef(mkSuite)) =>
        makeTasks(taskDef, mkSuite)
    }.unzip

    val globalResources = (tasksAndSuites.collect {
      case (_, suiteLoader.GlobalResourcesRef(init)) => init
    }.toSet ++ spiGlobalResources).toList

    // Passing a promise to the FP side that needs to be fulfilled
    // when the global resources have been allocated.
    val gate = Promise[Unit]()
    runBackground(globalResources,
                  waitForResourcesShutdown,
                  ioTasks.toList,
                  gate)
    stillRunning.set(sbtTasks.size)

    // Waiting for the resources to be allocated.
    scala.concurrent.blocking {
      scala.concurrent.Await.result(gate.future, 120.second)
    }
    sbtTasks.toArray
  }

  def serializeTask(task: Task, serializer: TaskDef => String): String =
    serializer(task.taskDef())

  def deserializeTask(
      task: String,
      deserializer: String => TaskDef): Task = {
    tasks(Array(deserializer(task))).head
  }

  private def run(
      globalResources: List[GlobalResourceF[F]],
      waitForResourcesShutdown: java.util.concurrent.Semaphore,
      tasks: List[IOTask],
      gate: Promise[Unit]): F[Unit] = {

    def preventDeadlock[A](resource: Resource[F, A]) = {
      CECompat.onErrorEnsure(resource) {
        error =>
          effect.delay(isDone.set(true)) *>
            effect.delay(error.printStackTrace(errorStream)) *>
            effect.delay(gate.failure(error))
      }
    }

    val cleanupResource = Resource.make(effect.unit)(_ =>
      effect.delay(waitForResourcesShutdown.release()))

    val globalResourcesWithCompletion =
      cleanupResource.flatMap(_ => resourceMap(globalResources))

    preventDeadlock(globalResourcesWithCompletion).use { read =>
      for {
        _   <- effect.delay(gate.success(()))
        ref <- Ref.of[F, Chain[(SuiteName, TestOutcome)]](Chain.empty)
        sem <- Semaphore[F](0L)
        maybePublish: F[Unit] =
          sem.release
            .productR(sem.tryAcquireN(tasks.size.toLong))
            .flatMap { isLast =>
              ref.get.flatMap { failed =>
                unsafeRun
                  .effect
                  .delay(self.failedOutcomes = failed)
              }.whenA(isLast)
            }
        _ <- tasks.parTraverse(_.run(read, ref, maybePublish))
      } yield ()
    }

  }

  private def resourceMap(
      globalResources: List[GlobalResourceF[F]]
  ): Resource[F, GlobalResourceF.Read[F]] =
    CECompat.resourceLift(GlobalResourceF.createMap[F]).flatTap { map =>
      globalResources.traverse(_.sharedResources(map)).void
    }

  private def spiGlobalResources: List[GlobalResourceF[F]] = {
    val result = ListBuffer.empty[GlobalResourceF[F]]
    val loader: ServiceLoader[GlobalResourceF[F]] = ServiceLoader.load(classOf[GlobalResourceF[F]])
    loader.iterator().forEachRemaining(result += _)
    result.toList
  }

  private case class IOTask(
      fqn: String,
      mkSuite: MakeSuite,
      args: List[String],
      start: F[Unit],
      broker: SuiteEventBroker) {
    def run(
        globalResources: GlobalResourceF.Read[F],
        outcomes: Ref[F, Chain[(SuiteName, TestOutcome)]],
        maybePublish: F[Unit]): F[Unit] = {

      val runSuite = for {
        suite <- mkSuite(globalResources)
        _     <- start // waiting for SBT to tell us to start
        _     <- broker.send(SuiteStarted(SuiteName(fqn)))
        _ <- suite.run(args) { testOutcome =>
          outcomes
            .update(_.append(SuiteName(fqn) -> testOutcome))
            .whenA(testOutcome.status.isFailed)
            .productR(broker.send(TestFinished(testOutcome)))
        }
      } yield ()

      val finalizer =
        maybePublish.productR(broker.send(SuiteFinished(SuiteName(fqn))))

      CECompat.guaranteeCase(runSuite)(
        completed = finalizer,
        cancelled = finalizer,
        errored = { (error: Throwable) =>
          val outcome =
            TestOutcome("Unexpected failure",
                        0.seconds,
                        Result.from(error),
                        Chain.empty)

          CECompat.guarantee(outcomes
            .update(_.append(SuiteName(fqn) -> outcome))
            .productR(broker.send(TestFinished(outcome))))(finalizer)
        }
      ).handleErrorWith { case scala.util.control.NonFatal(_) =>
        effect.unit // avoid non-fatal errors propagating up
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
}
