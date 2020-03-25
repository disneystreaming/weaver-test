package weaver

import cats.data.Chain

import scala.concurrent.duration.FiniteDuration

trait TestOutcome {
  def name: String
  def duration: FiniteDuration
  def status: Status
  def log: Chain[Log.Entry]
  def formatted: String
  def cause: Option[Throwable]
}

object TestOutcome {

  def apply(
      name: String,
      duration: FiniteDuration,
      result: Result,
      log: Chain[Log.Entry]): TestOutcome = Default(name, duration, result, log)

  case class Default(
      name: String,
      duration: FiniteDuration,
      result: Result,
      log: Chain[Log.Entry])
      extends TestOutcome {

    def status: Status = result match {
      case Result.Success                               => Status.Success
      case Result.Cancelled(_, _)                       => Status.Cancelled
      case Result.Ignored(_, _)                         => Status.Ignored
      case Result.Failure(_, _, _) | Result.Failures(_) => Status.Failure
      case Result.Exception(_, _)                       => Status.Exception
    }

    def cause: Option[Throwable] = result match {
      case Result.Exception(cause, _)       => Some(cause)
      case Result.Failure(_, maybeCause, _) => maybeCause
      case _                                => None
    }

    def formatted: String =
      Formatter.outcomeWithResult(this, result)
  }
}
