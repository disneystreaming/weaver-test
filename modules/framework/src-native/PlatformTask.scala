package weaver

import scala.concurrent.Await
import scala.concurrent.duration._

import sbt.testing.{ EventHandler, Logger, Task }

private[weaver] trait PlatformTask extends AsyncTask {

  override def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = {
    val future = executeFuture(eventHandler, loggers)
    scalanative.runtime.loop()
    Await.result(future, 5.minutes)
    Array.empty[Task]
  }
}
