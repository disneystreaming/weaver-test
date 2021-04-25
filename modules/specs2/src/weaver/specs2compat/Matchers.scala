package weaver.specs2compat

import cats.Monoid
import cats.data.Validated.{ Invalid, Valid }
import cats.data.{ NonEmptyList, Validated }
import cats.effect.IO

import weaver.{ AssertionException, EffectSuite, Expectations, SourceLocation }

import org.specs2.execute.{ Failure, Result, Success }
import org.specs2.matcher.{ MatchResult, MustMatchers, StandardMatchResults }

trait Matchers[F[_]] extends MustMatchers {
  self: EffectSuite[F] =>

  implicit def toExpectations[A](
      m: MatchResult[A]
  )(
      implicit pos: SourceLocation
  ): Expectations =
    if (m.toResult.isSuccess) {
      Monoid[Expectations].empty
    } else {
      Expectations(Validated.invalidNel(new AssertionException(
        m.toResult.message,
        NonEmptyList.of(pos))))
    }

  implicit def toExpectationsF[A](
      m: MatchResult[A]
  )(
      implicit pos: SourceLocation
  ): F[Expectations] = effectCompat.effect.pure(toExpectations(m))

  /** 
   * Some specs2 matchers expect a MatchResult/Result so we might have to convert them back from weaver's Expectations
   **/

  implicit def toSpecs2Result(ex: Expectations): Result =
    ex.run match {
      case Valid(_) =>
        Success("", "")
      case Invalid(err) =>
        Failure(err.head.message,
                err.head.message,
                err.head.getStackTrace().toList)
    }

  implicit def toSpecs2MatchResult(ex: Expectations): MatchResult[_] =
    ex.run match {
      case Valid(_) =>
        StandardMatchResults.ok
      case Invalid(err) =>
        StandardMatchResults.ko(err.head.message)
    }
}

trait IOMatchers extends Matchers[IO] {
  self: EffectSuite[IO] =>
}
