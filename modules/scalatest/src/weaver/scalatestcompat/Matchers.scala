package weaver.scalatestcompat

import cats.Monoid
import weaver.{ EffectSuite, Expectations, SourceLocation }
import weaver.Expectations.Helpers._
import org.scalatest.Assertion
import scala.util.{ Failure, Success, Try }

trait Matchers[F[_]] {
  self: EffectSuite[F] =>

  implicit def toExpectations(
      a: Assertion
  )(
      implicit pos: SourceLocation
  ): Expectations =
    Try(a) match {
      case Success(_) => Monoid[Expectations].empty
      case Failure(t) => failure(t.getMessage)
    }

  implicit def toExpectationsF(
      a: Assertion
  )(
      implicit pos: SourceLocation
  ): F[Expectations] = effectCompat.effect.pure(toExpectations(a))
}
