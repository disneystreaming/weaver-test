package weaver

sealed trait TestStatus {
  def isFailed = this match {
    case TestStatus.Success | TestStatus.Cancelled | TestStatus.Ignored => false
    case TestStatus.Failure | TestStatus.Exception                      => true
  }
}
object TestStatus {
  case object Success   extends TestStatus
  case object Cancelled extends TestStatus
  case object Ignored   extends TestStatus
  case object Failure   extends TestStatus
  case object Exception extends TestStatus
}
