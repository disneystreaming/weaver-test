package weaver
package testkit

import scala.util.control.NonFatal

sealed abstract class WeaverTestException(
    message: String,
    cause: Option[Throwable],
    location: SourceLocation)
    extends WeaverException(message, cause, location)

final class AssertionException(
    val message: String,
    val location: SourceLocation)
    extends WeaverTestException(message, None, location)

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
