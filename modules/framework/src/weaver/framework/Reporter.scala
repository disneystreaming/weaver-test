package weaver
package framework

import sbt.testing.Logger
import cats.data.Chain

object Reporter {
  import Colours._

  def logTestFinished(loggers: Array[Logger])(outcome: TestOutcome) = {
    loggers.foreach { logger =>
      logger.info(outcome.formatted(TestOutcome.Summary))
    }
  }

  def logSuiteStarted(loggers: Array[Logger])(suiteName: SuiteName) = {
    loggers.foreach { logger =>
      logger.info(cyan(suiteName.name))
    }
  }

  def logRunFinished(loggers: Array[Logger])(failed: Chain[(
      SuiteName,
      TestOutcome)]) = {
    if (failed.nonEmpty) {
      loggers.foreach(
        _.info(red("*************") + "FAILURES" + red("**************")))
    }
    failed.groupBy(_._1.name).foreach {
      case (suiteName, events) =>
        loggers.foreach(_.info(cyan(suiteName)))
        for ((_, event) <- events.iterator) {
          loggers.foreach(_.error(event.formatted(TestOutcome.Verbose)))
        }
        loggers.foreach(_.info(""))
    }
  }

  def log(loggers: Array[Logger])(event: SuiteEvent): Unit = event match {
    case SuiteStarted(name) =>
      logSuiteStarted(loggers)(name)
    case RunFinished(failed) =>
      logRunFinished(loggers)(failed)
    case TestFinished(outcome) =>
      logTestFinished(loggers)(outcome)

    case _: SuiteFinished => ()
  }
}
