package weaver.test

import cats.data.Chain
import cats.effect.IO
import cats.effect.concurrent.Ref
import cats.implicits._
import sbt.testing.{ Event => SbtEvent, _ }
import weaver._
import weaver.framework.TestFramework
import cats.kernel.Eq

object DogFoodSuite extends SimpleIOSuite {

  simpleTest("test suite reports successes events") {
    runSuite(Meta.MutableSuiteTest).map {
      case (_, events) => forall(events)(isSuccess)
    }
  }

  simpleTest(
    "the framework reports exceptions occurring during suite initialisation") {
    runSuite("weaver.test.Meta$CrashingSuite").map {
      case (logs, events) =>
        val errorLogs = logs.collect { case Error(msg) => msg }
        exists(events.headOption) { event =>
          val name = event.fullyQualifiedName()
          expect(name == "weaver.test.Meta$CrashingSuite") and
          expect(event.status() == Status.Error)
        } and exists(errorLogs) { log =>
          expect(log.contains("Unexpected failure")) and
          expect(log.contains("Boom"))
        }
    }
  }

  // todo: ensure none of these contain side effects
  def getTasks(fullyQualifiedName: String): Array[Task] = {
    val tf     = new TestFramework
    val runner = tf.runner(Array(), Array(), this.getClass.getClassLoader)
    val taskDefs: Array[TaskDef] = Array(
      new TaskDef(fullyQualifiedName,
                  TestFramework.ModuleFingerprint,
                  true,
                  Array(new SuiteSelector)))
    runner.tasks(taskDefs)
  }

  type State = (Chain[LoggedEvent], Chain[SbtEvent])

  // Method used to run a test-suite
  def runSuite(suiteName: String): IO[State] =
    for {
      logRef   <- Ref.of[IO, Chain[LoggedEvent]](Chain[LoggedEvent]())
      eventRef <- Ref.of[IO, Chain[SbtEvent]](Chain[SbtEvent]())
      eventHandler = new MemoryEventHandler(eventRef)
      logger       = new MemoryLogger(logRef)
      _      <- runTasks(eventHandler, logger)(getTasks(suiteName))
      logs   <- logRef.get
      events <- eventRef.get
    } yield (logs, events)

  // Method used to run a test-suite
  def runSuite(suite: EffectSuite[IO]): IO[State] =
    runSuite(suite.getClass.getName.dropRight(1))

  private def runTasks(eventHandler: EventHandler, logger: Logger)(
      tasks: Array[Task]): IO[Unit] =
    tasks.toVector.foldMap { task =>
      IO(task.execute(eventHandler, Array(logger)))
        .flatMap(runTasks(eventHandler, logger))
    }

  private def unsafeModifyRefChain[A](ref: Ref[IO, Chain[A]], el: A): Unit = {
    ref.update(chain => chain :+ el).unsafeRunSync()
  }

  class MemoryEventHandler(events: Ref[IO, Chain[SbtEvent]])
      extends EventHandler {
    override def handle(event: SbtEvent): Unit =
      unsafeModifyRefChain(events, event)
  }

  sealed trait LoggedEvent
  final case class Error(msg: String)  extends LoggedEvent
  final case class Warn(msg: String)   extends LoggedEvent
  final case class Info(msg: String)   extends LoggedEvent
  final case class Debug(msg: String)  extends LoggedEvent
  final case class Trace(t: Throwable) extends LoggedEvent
  object LoggedEvent {
    implicit val loggedEventEq: Eq[LoggedEvent] = Eq.fromUniversalEquals
  }
  class MemoryLogger(events: Ref[IO, Chain[LoggedEvent]]) extends Logger {
    override def ansiCodesSupported(): Boolean = false

    override def error(msg: String): Unit =
      unsafeModifyRefChain(events, Error(msg))

    override def warn(msg: String): Unit =
      unsafeModifyRefChain(events, Warn(msg))

    override def info(msg: String): Unit =
      unsafeModifyRefChain(events, Info(msg))

    override def debug(msg: String): Unit =
      unsafeModifyRefChain(events, Debug(msg))

    override def trace(t: Throwable): Unit =
      unsafeModifyRefChain(events, Trace(t))
  }

  def isSuccess(event: sbt.testing.Event)(
      implicit loc: SourceLocation): Expectations = {
    event.status() match {
      case Status.Success => success
      case status =>
        failure(
          s"${event.fullyQualifiedName()}:${event.selector()} failed with $status")
    }
  }
}

// The build tool will only detect and run top-level test suites. We can however nest objects
// that contain failing tests, to allow for testing the framework without failing the build
// because the framework will have ran the tests on its own.
object Meta {
  object MutableSuiteTest extends MutableSuiteTest

  object Boom extends Error("Boom") with scala.util.control.NoStackTrace
  object CrashingSuite extends SimpleIOSuite {
    throw Boom
  }
}
