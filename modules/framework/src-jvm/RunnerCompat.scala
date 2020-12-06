package weaver
package framework

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

import cats.data.Chain
import cats.effect.concurrent.{ Ref, Semaphore }
import cats.effect.{ Sync, _ }
import cats.syntax.all._

import sbt.testing.{ Task, TaskDef }

trait RunnerCompat[F[_]] { self: sbt.testing.Runner =>

  protected val suiteLoader: SuiteLoader[F]
  protected val unsafeRun: UnsafeRun[F]
  import unsafeRun._

  private type MakeSuite = GlobalResourceF.Read[F] => F[EffectSuite[F]]

  private var cancelToken: Option[unsafeRun.CancelToken] = None

  override def done(): String = {
    isDone.set(true)
    cancelToken.foreach(unsafeRun.cancel)
    System.lineSeparator()
  }

  // Required on js
  def receiveMessage(msg: String): Option[String] = None

  // Flag meant to be raised if build-tool call `done`
  protected val isDone: AtomicBoolean = new AtomicBoolean(false)

  private def runBackground(
      globalResources: List[GlobalResourceF[F]],
      tasks: List[IOTask]): Unit = {
    cancelToken = Some(unsafeRun.background(run(globalResources, tasks)))
  }

  def tasks(taskDefs: Array[TaskDef]): Array[Task] = {

    val tasksAndSuites = taskDefs.toList.map { taskDef =>
      taskDef -> suiteLoader(taskDef)
    }.collect { case (taskDef, Some(suite)) => (taskDef, suite) }

    def makeTasks(
        taskDef: TaskDef,
        mkSuite: MakeSuite): (IOTask, Task) = {
      val promise = scala.concurrent.Promise[Unit]()
      if (args().contains("--quickstart") || args().contains("-qs"))
        promise.success(())
      val queue  = new ConcurrentLinkedQueue[SuiteEvent]()
      val broker = new ConcurrentQueueEventBroker(queue)
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

      val sbtTask = new SbtTask(taskDef, isDone, promise, queue)
      (ioTask, sbtTask)
    }

    val (ioTasks, sbtTasks) = tasksAndSuites.collect[(IOTask, Task)] {
      case (taskDef, suiteLoader.SuiteRef(mkSuite)) =>
        makeTasks(taskDef, _ => mkSuite)
      case (taskDef, suiteLoader.ResourcesSharingSuiteRef(mkSuite)) =>
        makeTasks(taskDef, mkSuite)
    }.unzip

    val globalResources = tasksAndSuites.collect {
      case (_, suiteLoader.GlobalResourcesRef(init)) => init
    }.toList

    runBackground(globalResources, ioTasks.toList)

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
      globalResources: List[GlobalResourceF[F]]
  ): Resource[F, GlobalResourceF.Read[F]] =
    Resource.liftF(GlobalResourceF.createMap[F]).flatTap { map =>
      globalResources.traverse(_.sharedResources(map)).void
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
        semaphore: Semaphore[F],
        N: Long): F[Unit] = {

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

      val finalizer = semaphore
        .release
        .productR(semaphore.tryAcquireN(N))
        .flatMap {
          case true  => outcomes.get.map(RunFinished(_): SuiteEvent)
          case false => (SuiteFinished(SuiteName(fqn)): SuiteEvent).pure[F]
        }.flatMap(broker.send)

      effect.guaranteeCase(runSuite) {
        case ExitCase.Canceled  => finalizer
        case ExitCase.Completed => finalizer
        case ExitCase.Error(error: Throwable) =>
          val outcome =
            TestOutcome("Unexpected failure",
                        0.seconds,
                        Result.from(error),
                        Chain.empty)

          effect.guarantee(outcomes
            .update(_.append(SuiteName(fqn) -> outcome))
            .productR(broker.send(TestFinished(outcome))))(finalizer)

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
