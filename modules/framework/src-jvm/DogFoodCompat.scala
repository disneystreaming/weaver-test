package weaver
package framework

import cats.effect.Resource
import cats.syntax.all._

import sbt.testing._

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def blocker: BlockerCompat[F]

  def runTasksCompat(
      runner: WeaverRunner[F],
      eventHandler: EventHandler,
      logger: Logger)(tasks: List[sbt.testing.Task]): F[Unit] = {

    effect.void{
    tasks.toVector.parTraverse { task =>
      blocker.block(task.execute(
        eventHandler,
        Array(logger)))
    }}
  }

  def done(runner: Runner): F[String] =
    blocker.block(runner.done())
}

private[weaver] trait DogFoodCompanion {

  def make[F[_]](framework: WeaverFramework[F]): Resource[F, DogFood[F]] = {
    framework.unsafeRun.blocker { bl =>
      new DogFood[F](framework) {
        def blocker: BlockerCompat[F] = bl
      }
    }
  }
}
