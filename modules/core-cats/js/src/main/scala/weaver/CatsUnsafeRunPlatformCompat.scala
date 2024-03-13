package weaver

import cats.effect.IO

private[weaver] trait CatsUnsafeRunPlatformCompat {
  self: CatsUnsafeRun =>

  def unsafeRunSync(task: IO[Unit]): Unit = ???

  def background(task: IO[Unit]): CancelToken = ???

}
