package weaver
package framework

import sbt.testing.{
  Fingerprint,
  OptionalThrowable,
  Selector,
  Status,
  TestSelector,
  Event => BaseEvent,
  Task => BaseTask
}
import weaver._

trait WeaverTask extends BaseTask {

  def sbtEvent(event: Event): BaseEvent = new BaseEvent {

    private val task = taskDef()

    import event._

    def fullyQualifiedName(): String =
      task.fullyQualifiedName()

    def throwable(): OptionalThrowable =
      result match {
        case Result.Exception(cause, _) =>
          new OptionalThrowable(cause)
        case Result.Failure(_, Some(cause), _) =>
          new OptionalThrowable(cause)
        case _ =>
          new OptionalThrowable()
      }

    def status(): Status =
      result match {
        case Result.Exception(_, _) =>
          Status.Error
        case Result.Failure(_, _, _) =>
          Status.Failure
        case Result.Failures(_) =>
          Status.Failure
        case Result.Success =>
          Status.Success
        case Result.Ignored(_, _) =>
          Status.Ignored
        case Result.Cancelled(_, _) =>
          Status.Canceled
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
