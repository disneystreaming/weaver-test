package weaver
package framework

import cats.implicits._
import cats.effect.{ ContextShift, IO, Timer }
import cats.effect.concurrent.{ Ref, Semaphore }
import cats.data.Chain
import sbt.testing.{ Runner => BaseRunner, Task => BaseTask, _ }
import weaver.testkit.TestOutcome

final class Runner(
    val args: Array[String],
    val remoteArgs: Array[String],
    classLoader: ClassLoader)
    extends BaseRunner {

  def done(): String = ""

  val ec = scala.concurrent.ExecutionContext.global

  implicit val timerIO: Timer[IO]             = IO.timer(ec)
  implicit val contextShift: ContextShift[IO] = IO.contextShift(ec)

  def tasks(list: Array[TaskDef]): Array[BaseTask] = {
    val N = list.length.toLong

    val prep = for {
      semaphore <- Semaphore[IO](N)
      _         <- semaphore.acquireN(N)
      ref       <- Ref[IO].of(Chain.empty: Chain[(String, TestOutcome)])
    } yield {

      val next: IO[BaseTask] = IO(
        new ReportTask((f: Chain[(String, TestOutcome)] => IO[Unit]) =>
          for {
            acquired <- semaphore.tryAcquireN(N)
            log      <- ref.get
            _        <- if (acquired) f(log) else IO.unit
          } yield ())
      )

      val loggedBracket: LoggedBracket = withLogger =>
        withLogger((str, event) => ref.update(cat => cat.append(str -> event))) *> semaphore.release

      list.map { taskDef =>
        new Task(
          taskDef,
          classLoader,
          loggedBracket.some,
          next.map(_.some)
        ): BaseTask
      }
    }
    prep.unsafeRunSync()
  }

  def receiveMessage(msg: String): Option[String] = {
    discard[String](msg)
    None
  }

  def serializeTask(task: BaseTask, serializer: TaskDef => String): String =
    serializer(task.taskDef())

  def deserializeTask(task: String, deserializer: String => TaskDef): BaseTask =
    new Task(deserializer(task), classLoader, None, IO.pure(None))
}
