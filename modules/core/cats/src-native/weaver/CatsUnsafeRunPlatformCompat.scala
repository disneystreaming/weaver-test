package weaver

import cats.effect.IO
import scala.concurrent.Await
import scala.concurrent.duration._
import cats.effect.unsafe.implicits.global

private[weaver] trait CatsUnsafeRunPlatformCompat {
  self: CatsUnsafeRun =>

  def sync(task: IO[Unit]): Unit = {
    val future = task.unsafeToFuture()
    scalanative.runtime.loop()
    Await.result(future, 1.minute)
  }

  def background(task: IO[Unit]): CancelToken = ???

}
