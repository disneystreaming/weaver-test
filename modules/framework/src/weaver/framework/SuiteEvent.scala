package weaver
package framework

import cats.data.Chain

sealed trait SuiteEvent                             extends Product with Serializable
final case class SuiteStarted(name: SuiteName)      extends SuiteEvent
final case class TestFinished(outcome: TestOutcome) extends SuiteEvent
final case class SuiteFinished(
    name: SuiteName,
    failedOutcomes: Chain[(SuiteName, TestOutcome)])
    extends SuiteEvent
