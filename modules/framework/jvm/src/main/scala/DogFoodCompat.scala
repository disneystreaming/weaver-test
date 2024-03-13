package weaver
package framework

import cats.effect.Resource
import cats.effect.implicits._

import sbt.testing._
import org.typelevel.scalaccompat.annotation.unused

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def blocker: BlockerCompat[F]

  def runTasksCompat(
      @unused runner: WeaverRunner[F],
      eventHandler: EventHandler,
      logger: Logger,
      maxParallelism: Int)(tasks: List[sbt.testing.Task]): F[Unit] = {
    effect.void {
      val r = tasks.toVector.parTraverseN[F, Unit](maxParallelism) { task =>
        blocker.block(discard[Array[Task]](task.execute(eventHandler,
                                                        Array(logger))))
      }
      r
    }
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
