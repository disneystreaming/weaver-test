package weaver

import sbt.testing.{ EventHandler, Logger, Task }

private[weaver] trait PlatformTask extends AsyncTask {

  override def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger],
      continuation: Array[Task] => Unit): Unit = {
    val _ = executeFuture(eventHandler, loggers).map(_ =>
      continuation(Array.empty[Task]))(
      scala.scalajs.concurrent.JSExecutionContext.queue)
  }

  override def execute(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Array[Task] = Array.empty[Task]
}
