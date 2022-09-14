package weaver

import scala.concurrent.Future

import sbt.testing.{ EventHandler, Logger, Task }

private[weaver] trait AsyncTask extends Task {
  def executeFuture(
      eventHandler: EventHandler,
      loggers: Array[Logger]): Future[Unit]
}
