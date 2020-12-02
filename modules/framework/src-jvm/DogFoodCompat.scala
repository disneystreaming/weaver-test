package weaver
package framework

import cats.effect.{ Blocker, Resource }
import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def blocker: Blocker

  def runTasksCompat(
      runner: WeaverRunner[F],
      eventHandler: EventHandler,
      logger: Logger)(tasks: Array[SbtTask]): F[Unit] =
    tasks.toVector.traverse { task =>
      blocker.delay(task.execute(eventHandler, Array(logger)))
    }.void

  def done(runner: Runner): F[String] = blocker.delay[F, String](runner.done())
}

private[weaver] trait DogFoodCompanion {
  def make[F[_]](framework: WeaverFramework[F]): Resource[F, DogFood[F]] = {
    import framework.unsafeRun.effect

    Blocker[F].map { block =>
      new DogFood[F](framework) {
        override def blocker: Blocker = block
      }
    }
  }
}
