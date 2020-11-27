package weaver
package framework

import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }
import cats.effect.Blocker

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      eventHandler: EventHandler,
      logger: Logger,
      blocker: Blocker)(tasks: Array[SbtTask]): F[Unit] =
    tasks.toVector.traverse { task =>
      blocker.delay(task.execute(eventHandler, Array(logger)))
        .flatMap(runTasksCompat(eventHandler, logger, blocker))
    }.void
}
