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
        case Status.Exception => SbtStatus.Error
        case Status.Failure   => SbtStatus.Failure
        case Status.Success   => SbtStatus.Success
        case Status.Ignored   => SbtStatus.Ignored
        case Status.Cancelled => SbtStatus.Canceled
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
