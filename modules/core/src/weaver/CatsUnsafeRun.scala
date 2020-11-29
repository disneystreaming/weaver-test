package weaver

import cats.effect.{ IO, ContextShift }
import scala.concurrent.ExecutionContext
import cats.effect.Timer

object CatsUnsafeRun extends UnsafeRun[IO] {

  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  implicit val timer: Timer[IO] =
    IO.timer(ExecutionContext.global)

  implicit val effect = IO.ioConcurrentEffect
  implicit val parallel   = IO.ioParallel

  def void: IO[Unit] = IO.unit

  def background(task: IO[Unit]): IO[Unit] =
    task.unsafeRunCancelable {
      case Left(error) => error.printStackTrace
      case Right(_)    => ()
    }

  def sync(task: IO[Unit]): Unit = task.unsafeRunSync()

}
