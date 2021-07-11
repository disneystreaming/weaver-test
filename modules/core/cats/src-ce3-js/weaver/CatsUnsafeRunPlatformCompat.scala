package weaver

import cats.effect.IO

private[weaver] trait CatsUnsafeRunPlatformCompat {
  self: CatsUnsafeRun =>

  def sync[A](task: IO[A]): A = throw new Exception("Cannot block on JS!")

  def background(task: IO[Unit]): CancelToken = ???

}
