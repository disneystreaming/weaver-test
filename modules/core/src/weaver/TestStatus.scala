package weaver

sealed abstract class TestStatus(val label: String) {
  def isFailed = this match {
    case TestStatus.Success | TestStatus.Cancelled | TestStatus.Ignored => false
    case TestStatus.Failure | TestStatus.Exception                      => true
  }
}
object TestStatus {
  val values: List[TestStatus] =
    List(Success, Cancelled, Ignored, Failure, Exception)

  def fromString(s: String): Either[String, TestStatus] =
    values.find(_.label == s) match {
      case Some(value) => Right(value)
      case None        => Left(s"$s is not a valid test status")
    }

  case object Success   extends TestStatus("success")
  case object Cancelled extends TestStatus("cancelled")
  case object Ignored   extends TestStatus("ignored")
  case object Failure   extends TestStatus("failure")
  case object Exception extends TestStatus("exception")
}
