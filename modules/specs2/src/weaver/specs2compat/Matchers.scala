package weaver.specs2compat

import cats.Monoid
import cats.data.Validated.{Invalid, Valid}
import cats.data.{ NonEmptyList, Validated }
import cats.effect.IO

import weaver.{ AssertionException, EffectSuite, Expectations, SourceLocation }

import org.specs2.execute.{Failure, Success}
import org.specs2.matcher.{MatchResult, MustMatchers, ValueCheck}

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

  implicit def toValueCheck[T](
    ex: T => Expectations
  ): ValueCheck[T] = ex.andThen(e => e.run match {
    case Valid(_) => 
      Success("", "")
    case Invalid(err) =>
      Failure(err.head.message, err.head.message, err.head.getStackTrace().toList)
  })
}

trait IOMatchers extends Matchers[IO] {
  self: EffectSuite[IO] =>
}
