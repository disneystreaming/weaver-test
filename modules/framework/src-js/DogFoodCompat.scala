package weaver
package framework

import cats.data.Chain
import cats.effect.Resource
import cats.syntax.all._

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      runner: WeaverRunner[F],
      eventHandler: sbt.testing.EventHandler,
      logger: sbt.testing.Logger)(tasks: List[sbt.testing.Task]): F[Unit] = {
    tasks.traverse { task =>
      self.framework.unsafeRun.async {
        (cb: (Either[Throwable, Unit] => Unit)) =>
          task.execute(eventHandler, Array(logger), _ => cb(Right(())))
      }
    }.map { _ =>
      Reporter.logRunFinished(Array(logger))(
        Chain(runner.failedTests.toSeq: _*))
    }
  }

  def done(runner: sbt.testing.Runner): F[String] = effect.delay(runner.done())

}

private[weaver] trait DogFoodCompanion {
  def make[F[_]](framework: WeaverFramework[F]): Resource[F, DogFood[F]] = {
    import framework.unsafeRun.effect
    CECompat.resourceLift(effect.delay(new DogFood(framework) {}))
  }
}
