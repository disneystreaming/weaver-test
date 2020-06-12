package weaver
package framework

import cats.effect.IO
import cats.implicits._
import sbt.testing.{ Task => SbtTask, _ }

private[weaver] object DogFoodCompat {

  def runTasks(eventHandler: EventHandler, logger: Logger)(
      tasks: Array[SbtTask]): IO[Unit] = {
    val continuation: Array[SbtTask] => Unit = tasks =>
      runTasks(eventHandler, logger)(tasks).unsafeRunAsyncAndForget()

    tasks.toVector.foldMap { task =>
      IO(task.execute(eventHandler, Array(logger), continuation))
    }
  }
}
