package weaver
package framework

import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }
import cats.effect.Resource

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      eventHandler: EventHandler,
      logger: Logger)(tasks: Array[SbtTask]): F[Unit] = {
    tasks.toVector.parTraverse { task =>
      self.framework.unsafeRun.effect.delay(task.execute(eventHandler, Array(logger), _ => ()))
    }.void
  }

  def done(runner: Runner) = runner.done().pure[F].void
}

private[weaver] object DogFoodCompat {
  def make[F[_]](framework: WeaverFramework[F]): Resource[F, DogFood[F]] = {
    import framework.unsafeRun.effect
    Resource.liftF(effect.delay(new DogFood(framework) {}))
  }
}
