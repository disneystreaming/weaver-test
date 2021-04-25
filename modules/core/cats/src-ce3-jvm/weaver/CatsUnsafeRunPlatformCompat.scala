package weaver

import cats.effect.IO
import cats.effect.unsafe.implicits.global

private[weaver] trait CatsUnsafeRunPlatformCompat { self: CatsUnsafeRun =>

  def sync[A](task: IO[A]): A = task.unsafeRunSync()

  def background(task: IO[Unit]): CancelToken =
    task.start.unsafeRunSync()

}
