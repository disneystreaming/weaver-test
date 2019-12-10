package weaver
package scalacheck

import cats.implicits._
import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.Parallel

import org.scalacheck.Test.Parameters
import org.scalacheck.util.Pretty
import org.scalacheck.{ Arbitrary, Prop, Shrink, Test }
import org.scalacheck.Prop

import org.scalacheck.Gen
import cats.effect.IO

trait IOCheckers extends Checkers[IO] { self: BaseIOSuite =>

  override implicit def parallel: Parallel[IO] = IO.ioParallel

}

trait Checkers[F[_]] {
  self: EffectSuite[F] =>

  implicit def parallel: Parallel[F]

  /** ScalaCheck test parameters instance. */
  def checkConfig: Parameters = Test.Parameters.default.withWorkers(1)

  def checkAsync(prop: Prop)(implicit loc: SourceLocation): F[Expectations] =
    PropTest.check[F](checkConfig, prop).map { result =>
      val reason = Pretty.pretty(result)
      if (!result.passed) Expectations.Helpers.failure(reason)
      else Expectations.Helpers.success
    }

  implicit def asProp(e: Expectations): Prop = {
    e.run match {
      case Invalid(_) =>
        val label = Result.fromAssertion(e).formatted("Property: ")
        Prop.falsified.label(label)
      case Valid(_) =>
        Prop.proved
    }
  }

  implicit def asProp1(e: SingleExpectation): Prop =
    asProp(Expectations.fromSingle(e))

  implicit def asPropF[P](fp: F[P])(implicit view: P => Prop): Prop =
    effect
      .toIO(fp)
      .attempt
      .map {
        case Left(error)  => Prop.exception(error)
        case Right(value) => view(value)
      }
      .unsafeRunSync() // TODO https://github.bamtech.co/OSS/weaver-test/issues/18

  // format: off

  /**
   * Universal quantifier
   */
  def forall[A,P](gen: Gen[A])(f: A => P)
  (implicit
    p: P => Prop,
    s1: Shrink[A], pp1: A => Pretty
  ): F[Expectations] = checkAsync(Prop.forAll(gen)(f)(p, s1, pp1))

  def forall[A1,P](f: A1 => P)
    (implicit
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty
    ): F[Expectations] = checkAsync(Prop.forAll(f)(p, a1, s1, pp1))

  def forall[A1,A2,P](f: (A1,A2) => P)
    (implicit
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty
    ): F[Expectations] = checkAsync(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2))

  def forall[A1,A2,A3,P](f: (A1,A2,A3) => P)
    (implicit
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty
    ): F[Expectations] = checkAsync(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3))

  def forall[A1,A2,A3,A4,P](f: (A1,A2,A3,A4) => P)
    (implicit
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      a4: Arbitrary[A4], s4: Shrink[A4], pp4: A4 => Pretty
    ): F[Expectations] = checkAsync(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3, a4, s4, pp4))

  def forall[A1,A2,A3,A4,A5,P](f: (A1,A2,A3,A4,A5) => P)
    (implicit
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      a4: Arbitrary[A4], s4: Shrink[A4], pp4: A4 => Pretty,
      a5: Arbitrary[A5], s5: Shrink[A5], pp5: A5 => Pretty
    ): F[Expectations] = checkAsync(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3, a4, s4, pp4, a5, s5, pp5))

  def forall[A1,A2,A3,A4,A5,A6,P](f: (A1,A2,A3,A4,A5,A6) => P)
    (implicit
      p: P => Prop,
      a1: Arbitrary[A1], s1: Shrink[A1], pp1: A1 => Pretty,
      a2: Arbitrary[A2], s2: Shrink[A2], pp2: A2 => Pretty,
      a3: Arbitrary[A3], s3: Shrink[A3], pp3: A3 => Pretty,
      a4: Arbitrary[A4], s4: Shrink[A4], pp4: A4 => Pretty,
      a5: Arbitrary[A5], s5: Shrink[A5], pp5: A5 => Pretty,
      a6: Arbitrary[A6], s6: Shrink[A6], pp6: A6 => Pretty
    ): F[Expectations] = checkAsync(Prop.forAll(f)(p, a1, s1, pp1, a2, s2, pp2, a3, s3, pp3, a4, s4, pp4, a5, s5, pp5, a6, s6, pp6))
  // format: on

}
