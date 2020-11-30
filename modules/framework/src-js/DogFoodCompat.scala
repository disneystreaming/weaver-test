package weaver
package framework

import cats.effect.Resource
import cats.syntax.all._

import sbt.testing.{ Task => SbtTask, _ }
import cats.data.Chain

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def runTasksCompat(
      runner: WeaverRunner[F],
      eventHandler: EventHandler,
      logger: Logger)(tasks: Array[SbtTask]): F[Unit] = {
    tasks.toVector.traverse { task =>
      self.framework.unsafeRun.effect.async {
        cb: (Either[Throwable, Unit] => Unit) =>
          task.execute(eventHandler, Array(logger), _ => cb(Right(())))
      }
    }.map { _ =>
      Reporter.logRunFinished(Array(logger))(
        Chain(runner.failedTests.toSeq: _*))
    }
  }

  def done(runner: Runner): F[String] = effect.delay(runner.done())

}

private[weaver] object DogFoodCompat {
  def make[F[_]](framework: WeaverFramework[F]): Resource[F, DogFood[F]] = {
    import framework.unsafeRun.effect
    Resource.liftF(effect.delay(new DogFood(framework) {}))
  }
}
