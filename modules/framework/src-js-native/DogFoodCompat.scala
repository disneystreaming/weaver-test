package weaver
package framework

import cats.data.Chain
import cats.effect.Resource
import cats.syntax.all._

private[weaver] trait DogFoodCompat[F[_]] { self: DogFood[F] =>

  import self.framework.unsafeRun._

  def blocker: BlockerCompat[F]

  def runTasksCompat(
      runner: WeaverRunner[F],
      eventHandler: sbt.testing.EventHandler,
      logger: sbt.testing.Logger,
      maxParallelism: Int)(tasks: List[sbt.testing.Task]): F[Unit] = {
    tasks.traverse { task =>
      self.framework.unsafeRun.fromFuture {
        task.asInstanceOf[AsyncTask].executeFuture(eventHandler, Array(logger))
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
    Resource.eval(effect.delay(new DogFood(framework) {
      def blocker = new BlockerCompat[F] {
        // can't block on javascript obviously
        def block[A](thunk: => A): F[A] = effect.delay(thunk)
      }
    }))
  }

}
