package weaver

import scala.util.control.NonFatal
import cats.data.NonEmptyList

abstract class WeaverException(
    message: String,
    cause: Option[Throwable],
    location: SourceLocation)
    extends RuntimeException(message, cause.orNull) {

  def getLocation: SourceLocation = location

}

sealed abstract class WeaverTestException(
    message: String,
    cause: Option[Throwable],
    location: SourceLocation)
    extends WeaverException(message, cause, location)

final case class AssertionException(
    message: String,
    locations: NonEmptyList[SourceLocation])
    extends WeaverTestException(message, None, locations.head)

final class IgnoredException(
    val reason: Option[String],
    val location: SourceLocation)
    extends WeaverTestException(reason.orNull, None, location)

final class CanceledException(
    val reason: Option[String],
    val location: SourceLocation)
    extends WeaverTestException(reason.orNull, None, location)

object OurException {

  /**
   * Utility for pattern matching.
   */
  def unapply(ex: Throwable): Option[WeaverException] = ex match {
    case ref: WeaverTestException =>
      Some(ref)
    case _ =>
      None
  }
}

object NotOurException {

  /**
   * Utility for pattern matching.
   */
  def unapply(ex: Throwable) = ex match {
    case OurException(_) =>
      None
    case NonFatal(_) =>
      Some(ex)
    case _ =>
      None
  }
}
