package weaver
package framework

import cats.data.Chain
import cats.effect.IO

import sbt.testing.{ Logger => BaseLogger }

object ReportTask {

  def report(loggers: Array[BaseLogger])(
      logs: Chain[(String, TestOutcome)]): IO[Unit] = IO {
    if (logs.nonEmpty) {
      loggers.foreach(
        _.info(red("*************") + "FAILURES" + red("**************")))
    }
    logs.groupBy(_._1).foreach {
      case (suiteName, events) =>
        loggers.foreach(_.info(cyan(suiteName)))
        for ((_, event) <- events.iterator) {
          loggers.foreach(_.error(event.formatted(TestOutcome.Verbose)))
        }
        loggers.foreach(_.info(""))
    }
  }

}
