package weaver.specs2compat

import cats.Monoid
import cats.data.{ NonEmptyList, Validated }
import cats.effect.IO

import weaver.{ AssertionException, EffectSuite, Expectations, SourceLocation }

import org.specs2.matcher.{ MatchResult, MustMatchers }

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
  ): F[Expectations] = effect.pure {
    toExpectations(m)
  }

}

trait IOMatchers extends Matchers[IO] {
  self: EffectSuite[IO] =>
}
