package weaver
package framework

import cats.effect.IO
import cats.implicits._
import cats.data.Chain

import org.scalajs.testinterface.TestUtils
import sbt.testing.{ Logger => BaseLogger, Task => BaseTask, _ }

import scala.concurrent.duration._
import scala.concurrent.{ Await, Promise }
import scala.util.Try
import scala.util.control.NonFatal

final class Task(
    val task: TaskDef,
    args: List[String],
    cl: ClassLoader,
    maybeLoggedBracket: Option[LoggedBracket],
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

    val defaultLoggedBracket: LoggedBracket = withLogger =>
      withLogger((_, event) =>
        loggers.toList.traverse(doLog(event)) *> handle(event) *> IO.unit)

    val loggedBracket = maybeLoggedBracket.getOrElse(defaultLoggedBracket)

    loggedBracket
      .apply { log =>
        // format: off
        val reportSink: fs2.Pipe[IO, TestOutcome, Unit] = _.flatMap[IO, Unit] {
          case event @ TestOutcome(_, _, Result.Success | Result.Ignored(_, _) | Result.Cancelled(_, _), _) =>
            fs2.Stream.eval((loggers.toList.traverse(doLog(event)) *> handle(event) *> IO.unit))
          case event =>
            fs2.Stream.eval(handle(event) *> log(task.fullyQualifiedName(), event))
        }
        // format: on

        loadSuite(task.fullyQualifiedName(), cl).fold(IO.unit) { suite =>
          loggers.foreach(_.info(cyan(task.fullyQualifiedName())))

          suite
            .ioSpec(args)
            .through(reportSink)
            .compile
            .drain
            .map(_ => loggers.foreach(_.info(EOL)))
            .handleErrorWith {
              case NonFatal(e) => // Unexpected failure
                val event: TestOutcome =
                  TestOutcome("Unexpected error",
                              0.seconds,
                              Result.from(e),
                              Chain.empty)
                for {
                  _ <- reportError(eventHandler, e)
                  _ <- log(task.fullyQualifiedName(), event)
                } yield ()
            }
        }
      }
      .flatMap(_ => maybeNext)
      .unsafeRunAsync {
        case Right(nextTask) => continuation(nextTask.toArray)
        case Left(_)         => continuation(Array())
      }
  }

  def doLog(event: TestOutcome)(logger: BaseLogger): IO[Unit] = {
    IO(logger.info(event.formatted))
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

  def loadSuite(name: String, loader: ClassLoader): Option[EffectSuite[Any]] = {
    Try(TestUtils.loadModule(name, loader)).toOption
      .collect { case ref: EffectSuite[_] => ref }
  }

}
