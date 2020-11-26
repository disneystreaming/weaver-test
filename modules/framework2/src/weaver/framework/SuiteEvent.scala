package weaver
package framework

import cats.data.Chain

sealed trait SuiteEvent
case class SuiteStarted(name: String)         extends SuiteEvent
case class TestFinished(outcome: TestOutcome) extends SuiteEvent
case class SuiteFinished(name: String)        extends SuiteEvent
case class RunFinished(failedOutcomes: Chain[(SuiteName, TestOutcome)])
    extends SuiteEvent
