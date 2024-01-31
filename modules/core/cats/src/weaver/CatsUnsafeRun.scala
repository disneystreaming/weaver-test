package weaver

import scala.concurrent.Future

import cats.effect.unsafe.implicits.global
import cats.effect.IO

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] with CatsUnsafeRunPlatformCompat {

  type CancelToken = () => Future[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO

  def unsafeRunAndForget(task: IO[Unit]): Unit = task.unsafeRunAndForget()
  def unsafeRunToFuture(task: IO[Unit]): Future[Unit] = task.unsafeToFuture()

}
