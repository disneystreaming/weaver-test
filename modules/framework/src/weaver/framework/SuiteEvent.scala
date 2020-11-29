package weaver
package framework

import cats.data.Chain

sealed trait SuiteEvent                             extends Product with Serializable
final case class SuiteStarted(name: String)         extends SuiteEvent
final case class TestFinished(outcome: TestOutcome) extends SuiteEvent
final case class SuiteFinished(name: String)        extends SuiteEvent
final case class RunFinished(failedOutcomes: Chain[(SuiteName, TestOutcome)])
    extends SuiteEvent
