package weaver
package framework

import cats.syntax.option._
import cats.data.Chain
import cats.effect.{ IO, Timer }
import cats.effect.concurrent.Ref
import sbt.testing.{
  Event => SbtEvent,
  Task => SbtTask,
  Status => SbtStatus,
  _
}
import weaver._, Platform._
import cats.kernel.Eq
import scala.concurrent.duration._

// Functionality to test how the frameworks react to successful and failing tests/suites
trait DogFood {
  type State = (Chain[LoggedEvent], Chain[SbtEvent])

  private val timer: Timer[IO] =
    IO.timer(scala.concurrent.ExecutionContext.global)

  // ScalaJS executes asynchronously, therefore we need to wait
  // for some time before getting the logs back. On JVM platform
  // we do not need to wait, since the suite will run synchronously
  private val patience: Option[FiniteDuration] = PlatformCompat.platform match {
    case JS  => 2.seconds.some
    case JVM => none
  }

  // Method used to run a test-suite
  def runSuite(suiteName: String) =
    for {
      logRef   <- Ref.of[IO, Chain[LoggedEvent]](Chain[LoggedEvent]())
      eventRef <- Ref.of[IO, Chain[SbtEvent]](Chain[SbtEvent]())
      eventHandler = new DogFood.MemoryEventHandler(eventRef)
      logger       = new DogFood.MemoryLogger(logRef)
      _      <- runTasks(eventHandler, logger)(getTasks(suiteName))
      _      <- patience.fold(IO.unit)(timer.sleep)
      logs   <- logRef.get
      events <- eventRef.get
    } yield (logs, events)

  // Method used to run a test-suite
  def runSuite[F[_]](suite: EffectSuite[F]): IO[State] =
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

  // todo: ensure none of these contain side effects
  private def getTasks(fullyQualifiedName: String): Array[SbtTask] = {
    val tf     = new TestFramework
    val runner = tf.runner(Array(), Array(), new ClassLoader() {})
    val taskDefs: Array[TaskDef] = Array(
      new TaskDef(fullyQualifiedName,
                  TestFramework.ModuleFingerprint,
                  true,
                  Array(new SuiteSelector)))
    runner.tasks(taskDefs)
  }

  private def runTasks(eventHandler: EventHandler, logger: Logger)(
      tasks: Array[SbtTask]): IO[Unit] =
    DogFoodCompat.runTasks(eventHandler, logger)(tasks)

}

object DogFood extends DogFood {

  private[framework] class MemoryLogger(events: Ref[IO, Chain[LoggedEvent]])
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

  private def unsafeModifyRefChain[A](ref: Ref[IO, Chain[A]], el: A): Unit = {
    ref.update(chain => chain :+ el).unsafeRunSync()
  }

  private[framework] class MemoryEventHandler(events: Ref[IO, Chain[SbtEvent]])
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
