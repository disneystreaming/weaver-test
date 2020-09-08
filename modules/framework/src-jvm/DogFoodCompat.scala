package weaver
package framework

import cats.effect.IO
import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }

private[weaver] object DogFoodCompat {

  def runTasks(eventHandler: EventHandler, logger: Logger)(
      tasks: Array[SbtTask]): IO[Unit] =
    tasks.toVector.foldMap { task =>
      IO(task.execute(eventHandler, Array(logger)))
        .flatMap(runTasks(eventHandler, logger))
    }
}
