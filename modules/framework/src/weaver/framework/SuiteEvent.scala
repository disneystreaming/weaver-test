package weaver
package framework

import cats.data.Chain

// format: off

sealed trait RunEvent                               extends Product with Serializable
sealed trait SuiteEvent                             extends Product with Serializable
final case class SuiteStarted(name: SuiteName)      extends SuiteEvent with RunEvent
final case class TestFinished(outcome: TestOutcome) extends SuiteEvent with RunEvent
final case class SuiteFinished(name: SuiteName)     extends SuiteEvent with RunEvent
final case class RunFinished(failed: Chain[(SuiteName, TestOutcome)]) extends RunEvent
