package weaver
package framework

import sbt.testing.{
  Event,
  Fingerprint,
  OptionalThrowable,
  Selector,
  Status,
  TaskDef,
  TestSelector
}

object SbtEvent {
  def apply(td: TaskDef, outcome: TestOutcome): Event = new Event {

    private val task = td

    def fullyQualifiedName(): String =
      task.fullyQualifiedName()

    def throwable(): OptionalThrowable = outcome.cause match {
      case Some(value) => new OptionalThrowable(value)
      case None        => new OptionalThrowable()
    }

    def status(): Status =
      outcome.status match {
        case TestStatus.Exception => Status.Error
        case TestStatus.Failure   => Status.Failure
        case TestStatus.Success   => Status.Success
        case TestStatus.Ignored   => Status.Ignored
        case TestStatus.Cancelled => Status.Canceled
      }

    def selector(): Selector = {
      new TestSelector(outcome.name)
    }

    def fingerprint(): Fingerprint =
      task.fingerprint()

    def duration(): Long =
      outcome.duration.toMillis
  }
}
