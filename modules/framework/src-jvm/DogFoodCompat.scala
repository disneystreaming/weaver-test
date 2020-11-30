package weaver
package framework

import cats.syntax.all._
import cats.effect.Blocker
import cats.effect.Resource

import sbt.testing.{ Task => SbtTask, _ }

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def blocker: Blocker

  def runTasksCompat(
      eventHandler: EventHandler,
      logger: Logger)(tasks: Array[SbtTask]): F[Unit] =
    tasks.toVector.parTraverse { task =>
      blocker.delay(task.execute(eventHandler, Array(logger)))
    }.void

  def done(runner: Runner) = blocker.delay[F, String](runner.done()).void
}

private[weaver] object DogFoodCompat {
  def make[F[_]](framework: WeaverFramework[F]): Resource[F, DogFood[F]] = {
    import framework.unsafeRun.effect

    Blocker[F].map { block =>
      new DogFood[F](framework) {

        override def blocker: Blocker = block
      }
    }
  }
}
