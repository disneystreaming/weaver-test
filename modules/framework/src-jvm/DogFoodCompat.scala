package weaver
package framework

import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(eventHandler: EventHandler, logger: Logger)(
      tasks: Array[SbtTask]): F[Unit] =
    tasks.toVector.traverse { task =>
      concurrent.delay(task.execute(eventHandler, Array(logger)))
        .flatMap(runTasksCompat(eventHandler, logger))
    }.void
}
