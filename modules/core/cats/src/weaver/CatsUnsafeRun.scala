package weaver

import cats.effect.unsafe.implicits.global
import cats.effect.{ FiberIO, IO }
import scala.concurrent.Future

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] with CatsUnsafeRunPlatformCompat {

  type CancelToken = FiberIO[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO

  def cancel(token: CancelToken): Unit = unsafeRunSync(token.cancel)

  def unsafeRunAndForget(task: IO[Unit]): Unit = task.unsafeRunAndForget()
  def unsafeRunToFuture(task: IO[Unit]): Future[Unit] = task.unsafeToFuture()

}
