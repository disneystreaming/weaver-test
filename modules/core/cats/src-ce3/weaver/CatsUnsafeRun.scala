package weaver

import cats.effect.unsafe.implicits.global

import cats.effect.{ IO, FiberIO }

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] {

  type CancelToken = FiberIO[Unit]

  override implicit val parallel = IO.parallelForIO
  override implicit val effect   = IO.asyncForIO

  def background(task: IO[Unit]): CancelToken =
    task.start.unsafeRunSync()

  def cancel(token: CancelToken): Unit = sync(token.cancel)

  def sync(task: IO[Unit]): Unit = task.unsafeRunSync()

  def async(task: IO[Unit]): Unit = task.unsafeRunAndForget()

}
