package weaver
package framework

import scala.concurrent.duration._
import scala.reflect.ClassTag

import cats.data.Chain
import cats.effect.concurrent.Ref
import cats.kernel.Eq
import cats.syntax.all._

import sbt.testing.{
  Event => SbtEvent,
  Status => SbtStatus,
  Task => SbtTask,
  _
}

// import Platform._
import cats.effect.Resource
import cats.effect.Sync
import cats.effect.syntax.all._

// Functionality to test how the frameworks react to successful and failing tests/suites
class DogFood[F[_]](val framework: WeaverFramework[F])
    extends DogFoodCompat[F] {
  import framework.unsafeRun._

  type State = (Chain[LoggedEvent], Chain[SbtEvent])

  // ScalaJS executes asynchronously, therefore we need to wait
  // for some time before getting the logs back. On JVM platform
  // we do not need to wait, since the suite will run synchronously
  // private val patience: Option[FiniteDuration] = PlatformCompat.platform match {
  //   case JS  => 2.seconds.some
  //   case JVM => none
  // }

  // // Method used to run test-suites
  def runSuites(suites: Fingerprinted*): F[State] = {
    for {
      logRef   <- Ref.of[F, Chain[LoggedEvent]](Chain[LoggedEvent]())
      eventRef <- Ref.of[F, Chain[SbtEvent]](Chain[SbtEvent]())
      eventHandler = new MemoryEventHandler(eventRef)
      logger       = new MemoryLogger(logRef)
      _ <- getTasks(suites)
        .use(runTasks(eventHandler, logger))
        .race(timer.sleep(2.seconds).as(Sync[F].delay(println("raced !")))) // TODO investigate why this deadlocks without the race ? (possible mis-use of thread pool ?)
      logs   <- logRef.get
      events <- eventRef.get
    } yield {
      (logs, events)
    }
  }

  // Method used to run a test-suite
  def runSuite(suiteName: String): F[State] =
    runSuites(Fingerprinted.ModuleSuite(suiteName))

  // Method used to run a test-suite
  def runSuite(suite: EffectSuite[F]): F[State] =
    runSuite(suite.getClass.getName.dropRight(1))

  def isSuccess(event: sbt.testing.Event)(
      implicit loc: SourceLocation): Expectations = {
    event.status() match {
      case SbtStatus.Success => Expectations.Helpers.success
      case status =>
        Expectations.Helpers.failure(
          s"${event.fullyQualifiedName()}:${event.selector()} failed with $status")
    }
  }

  private def getTasks(
      suites: Seq[Fingerprinted]): Resource[F, Array[SbtTask]] =
    Resource.make(Sync[F].delay {
      val cl = PlatformCompat.getClassLoader(this.getClass())
      framework.runner(Array(), Array(), cl)
    })(runner => Sync[F].delay(runner.done()).void).evalMap { runner =>
      val taskDefs: Array[TaskDef] = suites.toArray.map { s =>
        new TaskDef(s.fullyQualifiedName,
                    s.fingerprint,
                    true,
                    Array(new SuiteSelector))
      }
      Sync[F].delay(runner.tasks(taskDefs))
    }

  private def runTasks(eventHandler: EventHandler, logger: Logger)(
      tasks: Array[SbtTask]): F[Unit] =
    runTasksCompat(eventHandler, logger)(tasks)

  def globalInit(g: GlobalResourcesInit[F]): Fingerprinted =
    Fingerprinted.GlobalInit(g.getClass.getName.dropRight(1))
  def moduleSuite(g: EffectSuite[F]): Fingerprinted =
    Fingerprinted.ModuleSuite(g.getClass.getName.dropRight(1))
  def sharingSuite[S <: BaseSuiteClass](
      implicit ct: ClassTag[S]): Fingerprinted =
    Fingerprinted.SharingSuite(ct.runtimeClass.getName())

  sealed trait Fingerprinted {
    import framework.fp

    def fullyQualifiedName: String
    def fingerprint: fp.WeaverFingerprint = {
      import Fingerprinted._
      this match {
        case ModuleSuite(_)  => fp.SuiteFingerprint
        case GlobalInit(_)   => fp.GlobalResourcesFingerprint
        case SharingSuite(_) => fp.ResourceSharingSuiteFingerprint
      }
    }
  }
  private object Fingerprinted {
    case class ModuleSuite(fullyQualifiedName: String)  extends Fingerprinted
    case class GlobalInit(fullyQualifiedName: String)   extends Fingerprinted
    case class SharingSuite(fullyQualifiedName: String) extends Fingerprinted
  }

  private class MemoryLogger(events: Ref[F, Chain[LoggedEvent]])
      extends Logger {
    override def ansiCodesSupported(): Boolean = false

    override def error(msg: String): Unit =
      unsafeModifyRefChain(events, LoggedEvent.Error(msg))

    override def warn(msg: String): Unit =
      unsafeModifyRefChain(events, LoggedEvent.Warn(msg))

    override def info(msg: String): Unit =
      unsafeModifyRefChain(events, LoggedEvent.Info(msg))

    override def debug(msg: String): Unit =
      unsafeModifyRefChain(events, LoggedEvent.Debug(msg))

    override def trace(t: Throwable): Unit =
      unsafeModifyRefChain(events, LoggedEvent.Trace(t))
  }

  private def unsafeModifyRefChain[A](
      ref: Ref[F, Chain[A]],
      el: A): Unit = {
    framework.unsafeRun.sync(ref.update(chain => chain :+ el))
  }

  private class MemoryEventHandler(
      events: Ref[F, Chain[SbtEvent]])
      extends EventHandler {
    override def handle(event: SbtEvent): Unit =
      unsafeModifyRefChain(events, event)
  }

}

sealed trait LoggedEvent
object LoggedEvent {
  final case class Error(msg: String)  extends LoggedEvent
  final case class Warn(msg: String)   extends LoggedEvent
  final case class Info(msg: String)   extends LoggedEvent
  final case class Debug(msg: String)  extends LoggedEvent
  final case class Trace(t: Throwable) extends LoggedEvent

  implicit val loggedEventEq: Eq[LoggedEvent] = Eq.fromUniversalEquals
}
