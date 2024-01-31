package weaver

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import java.util.concurrent.atomic.AtomicBoolean

private[weaver] trait CatsUnsafeRunPlatformCompat { self: CatsUnsafeRun =>

  def unsafeRunSync(task: IO[Unit]): Unit = task.unsafeRunSync()

  def cancel(token: CancelToken): Unit =
    scala.concurrent.Await.result(token(), 10.seconds)

  def background(task: IO[Unit], isDone: AtomicBoolean): CancelToken = {
    val (future, cancelToken) = task.unsafeToFutureCancelable()
    future.onComplete {
      case Failure(_) => isDone.set(true)
      case Success(_) => ()
    }(scala.concurrent.ExecutionContext.global)
    cancelToken
  }

}
