package weaver

import cats.effect.unsafe.implicits.global
import cats.effect.{ FiberIO, IO }

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] with CatsUnsafeRunPlatformCompat {

  type CancelToken = FiberIO[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO

  def cancel(token: CancelToken): Unit = sync(token.cancel)

  def async(task: IO[Unit]): Unit = task.unsafeRunAndForget()

}
