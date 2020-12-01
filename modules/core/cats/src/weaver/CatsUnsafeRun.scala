package weaver

import scala.concurrent.ExecutionContext

import cats.effect.{ ContextShift, IO, Timer }

object CatsUnsafeRun extends UnsafeRun[IO] {

  override implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  override implicit val timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  override implicit val effect   = IO.ioConcurrentEffect(contextShift)
  override implicit val parallel = IO.ioParallel(contextShift)

  def void: IO[Unit] = IO.unit

  def background(task: IO[Unit]): IO[Unit] =
    task.unsafeRunCancelable {
      case Left(error) => error.printStackTrace
      case Right(_)    => ()
    }

  def sync(task: IO[Unit]): Unit = task.unsafeRunSync()

  def async(task: IO[Unit]): Unit = task.unsafeRunAsyncAndForget()

}
