package weaver
package framework

import sbt.testing.{
  Fingerprint,
  OptionalThrowable,
  Selector,
  TestSelector,
  Event => SbtEvent,
  Task => SbtTask,
  Status => SbtStatus
}

trait WeaverTask extends SbtTask {

  def sbtEvent(event: Event): SbtEvent = new SbtEvent {

    private val task = taskDef()

    def fullyQualifiedName(): String =
      task.fullyQualifiedName()

    def throwable(): OptionalThrowable = event.cause match {
      case Some(value) => new OptionalThrowable(value)
      case None        => new OptionalThrowable()
    }

    def status(): SbtStatus =
      event.status match {
        case TestStatus.Exception => SbtStatus.Error
        case TestStatus.Failure   => SbtStatus.Failure
        case TestStatus.Success   => SbtStatus.Success
        case TestStatus.Ignored   => SbtStatus.Ignored
        case TestStatus.Cancelled => SbtStatus.Canceled
      }

    def selector(): Selector = {
      new TestSelector(event.name)
    }

    def fingerprint(): Fingerprint =
      task.fingerprint()

    def duration(): Long =
      event.duration.toMillis
  }

}
