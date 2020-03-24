package weaver
package framework

import cats.effect.IO
import cats.implicits._
import cats.data.Chain

import org.scalajs.testinterface.TestUtils
import sbt.testing.{ Logger => BaseLogger, Task => BaseTask, _ }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Promise }
import scala.util.control.NonFatal
import scala.util.control.NoStackTrace
import cats.effect.Resource

final class Task(
    val task: TaskDef,
    args: List[String],
    cl: ClassLoader,
    maybeDeferredLogger: Option[Resource[IO, DeferredLogger]],
    maybeNext: IO[Option[BaseTask]])
    extends WeaverTask {

  def tags(): Array[String] = Array.empty
  def taskDef(): TaskDef    = task
  val EOL                   = scala.util.Properties.lineSeparator

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger],
      continuation: Array[BaseTask] => Unit): Unit = {

    def handle(event: TestOutcome): IO[Unit] =
      IO(eventHandler.handle(sbtEvent(event)))

    def doLog(event: TestOutcome): IO[Unit] =
      loggers.toVector.foldMap(logger => IO(logger.info(event.formatted)))

    val defaultLoggedBracket: Resource[IO, DeferredLogger] =
      Resource.pure((_, event) => doLog(event) *> handle(event))

    val loggerResource = maybeDeferredLogger.getOrElse(defaultLoggedBracket)

    // format: off
    loggerResource.use { log =>
      def report(event: TestOutcome) : IO[Unit] = IO.suspend {
        event match {
          case event if ! event.status.isFailed =>
            doLog(event) *> handle(event)
          case event =>
            handle(event) *> log(task.fullyQualifiedName(), event)
        }
      }
        // format: on

      loadSuite(task.fullyQualifiedName(), cl)
        .flatMap { suite =>
          loggers.foreach(_.info(cyan(task.fullyQualifiedName())))
          suite
            .run(args)(report)
            .map(_ => loggers.foreach(_.info(EOL)))
        }
        .handleErrorWith {
          case NonFatal(e) =>
            val event: TestOutcome =
              TestOutcome("Unexpected failure",
                          0.seconds,
                          Result.from(e),
                          Chain.empty)
            for {
              _ <- reportError(eventHandler, e)
              _ <- log(task.fullyQualifiedName(), event)
            } yield ()
        }
    }.flatMap(_ => maybeNext).unsafeRunAsync {
      case Right(nextTask) => continuation(nextTask.toArray)
      case Left(_)         => continuation(Array())
    }
  }

  def reportError(eventHandler: EventHandler, t: Throwable): IO[Unit] = IO {
    val errorEvent = new sbt.testing.Event {
      def fullyQualifiedName(): String   = task.fullyQualifiedName()
      def duration(): Long               = 0
      def fingerprint(): Fingerprint     = task.fingerprint()
      def status(): Status               = sbt.testing.Status.Error
      def throwable(): OptionalThrowable = new OptionalThrowable(t)
      def selector(): Selector           = new SuiteSelector
    }
    eventHandler.handle(errorEvent)
  }

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger]): Array[BaseTask] = {
    val p = Promise[Array[BaseTask]]()
    execute(eventHandler, loggers, tasks => p.success(tasks))
    Await.result(p.future, Duration.Inf)
  }

  def loadSuite(name: String, loader: ClassLoader): IO[EffectSuite[Any]] =
    IO(TestUtils.loadModule(name, loader)).flatMap {
      case ref: EffectSuite[_] => IO.pure(ref)
      case other =>
        IO.raiseError {
          new Exception(s"$other is not an effect suite") with NoStackTrace
        }
    }

}
