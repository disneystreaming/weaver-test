package weaver

import cats.effect.IO
import java.util.concurrent.atomic.AtomicBoolean

private[weaver] trait CatsUnsafeRunPlatformCompat {
  self: CatsUnsafeRun =>

  def unsafeRunSync(task: IO[Unit]): Unit = ???

  def background(task: IO[Unit], atomicBoolean: AtomicBoolean): CancelToken =
    ???

  def cancel(token: CancelToken): Unit = ???

}
