package weaver
package framework

import cats.data.Chain

import sbt.testing.Logger

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

  def runFinished(
      info: String => Unit,
      error: String => Unit)(failed: Chain[(SuiteName, TestOutcome)]) = {
    if (failed.nonEmpty) {
      info(red("*************") + "FAILURES" + red("**************"))
    }
    failed.groupBy(_._1.name).foreach {
      case (suiteName, events) =>
        info(cyan(suiteName))
        for ((_, event) <- events.iterator) {
          error(event.formatted(TestOutcome.Verbose))
        }
        info("")
    }
  }

  def logRunFinished(loggers: Array[Logger])(failed: Chain[(
      SuiteName,
      TestOutcome)]) = {

    runFinished(
      info = s => loggers.foreach(_.info(s)),
      error = s => loggers.foreach(_.error(s))
    )(failed)
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
