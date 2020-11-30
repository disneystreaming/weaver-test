package weaver
package framework

import cats.effect.Blocker
import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      eventHandler: EventHandler,
      logger: Logger,
      blocker: Blocker)(tasks: Array[SbtTask]): F[Unit] =
    tasks.toVector.parTraverse { task =>
      blocker.delay(task.execute(eventHandler, Array(logger)))
    }.void
}
