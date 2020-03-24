package weaver

sealed trait Status {
  def isFailed = this match {
    case Status.Success | Status.Cancelled | Status.Ignored => false
    case Status.Failure | Status.Exception                  => true
  }
}
object Status {
  case object Success   extends Status
  case object Cancelled extends Status
  case object Ignored   extends Status
  case object Failure   extends Status
  case object Exception extends Status
}
