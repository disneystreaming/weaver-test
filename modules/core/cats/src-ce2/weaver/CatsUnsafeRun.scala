package weaver

import scala.concurrent.ExecutionContext

import cats.effect.{ ContextShift, IO, Timer }

object CatsUnsafeRun extends CatsUnsafeRun

trait CatsUnsafeRun extends UnsafeRun[IO] {

  type CancelToken = IO[Unit]

  override implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  override implicit val timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  override implicit val effect   = IO.ioConcurrentEffect(contextShift)
  override implicit val parallel = IO.ioParallel(contextShift)

  def background(task: IO[Unit]): CancelToken =
    task.unsafeRunCancelable { _ => () }

  def cancel(token: CancelToken): Unit = sync(token)

  def sync[A](task: IO[A]): A = task.unsafeRunSync()

  def async(task: IO[Unit]): Unit = task.unsafeRunAsyncAndForget()

}
