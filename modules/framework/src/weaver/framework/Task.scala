package weaver
package framework

import scala.concurrent.duration._
import scala.util.control.NonFatal

import cats.data.Chain
import cats.effect.{ IO, Resource }
import cats.syntax.all._

import sbt.testing.{ Logger => BaseLogger, Task => BaseTask, _ }

import IO.ioMonoid

final class Task(
    val task: TaskDef,
    args: List[String],
    loadSuite: IO[EffectSuite[Any]],
    maybeDeferredLogger: Option[Resource[IO, DeferredLogger]],
    maybeNext: Option[Array[BaseLogger] => IO[Unit]])
    extends WeaverTask {

  def tags(): Array[String] = Array.empty
  def taskDef(): TaskDef    = task
  val EOL                   = TaskCompat.lineSeparator

  def execute(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger],
      continuation: Array[BaseTask] => Unit): Unit = {
    executeWrapper(eventHandler, loggers)
      .map(continuation)
      .unsafeRunAsyncAndForget()
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
    executeWrapper(eventHandler, loggers).unsafeRunSync()
  }

  private def executeWrapper(
      eventHandler: EventHandler,
      loggers: Array[BaseLogger]): IO[Array[BaseTask]] = {

    def handle(event: TestOutcome): IO[Unit] =
      IO(eventHandler.handle(sbtEvent(event)))

    def doLog(event: TestOutcome): IO[Unit] =
      loggers.toVector.foldMap { logger =>
        val formattingMode =
          if (maybeDeferredLogger.isDefined) TestOutcome.Summary
          else TestOutcome.Verbose
        IO(logger.info(event.formatted(formattingMode)))
      }

    val defaultLoggedBracket: Resource[IO, DeferredLogger] =
      Resource.pure[IO, DeferredLogger]((_, event) =>
        doLog(event) *> handle(event))

    val loggerResource = maybeDeferredLogger.getOrElse(defaultLoggedBracket)

    val finalize = maybeNext.foldMap(_(loggers))

    // format: off
    loggerResource.use { log =>
      def report(event: TestOutcome) : IO[Unit] = IO.suspend {
        event match {
          case event if ! event.status.isFailed =>
            doLog(event) *> handle(event)
          case event =>
            doLog(event) *> handle(event) *> log(task.fullyQualifiedName(), event)
        }
      }
      // format: on

      loadSuite
        .flatMap { suite =>
          loggers.foreach(_.info(cyan(task.fullyQualifiedName())))
          suite
            .runIO(args)(report)
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
    }.guarantee(finalize).attempt.as(Array.empty[BaseTask])
  }

}
