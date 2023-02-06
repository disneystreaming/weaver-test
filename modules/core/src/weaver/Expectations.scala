package weaver

import cats._
import cats.data.Validated._
import cats.data.{ NonEmptyList, Validated, ValidatedNel }
import cats.effect.Sync
import cats.syntax.all._

case class Expectations(run: ValidatedNel[AssertionException, Unit]) {
  self =>

  /**
   * Logical AND combinator for two `Expectations`
   *
   * @example
   *   {{{
   *     // success
   *     expect.eql("foo", "foo") and
   *       expect.eql("bar", "bar")
   *   }}}
   *
   * @param other
   *   The second operand
   */
  def and(other: Expectations): Expectations =
    Expectations(self.run.product(other.run).void)

  /**
   * Symbolic alias for `and`
   *
   * @example
   *   {{{
   *     // success
   *     expect.eql("foo", "foo") &&
   *       expect.eql("bar", "bar")
   *   }}}
   *
   * @param other
   *   The second operand
   */
  def &&(other: Expectations): Expectations = and(other)

  /**
   * Logical OR combinator for two `Expectations`
   *
   * @example
   *   {{{
   *     // success
   *     expect.eql("foo", "bar") or
   *       expect.eql("alpha", "alpha")
   *   }}}
   *
   * @param other
   *   The second operand
   */
  def or(other: Expectations): Expectations =
    Expectations(
      self.run
        .orElse(other.run)
        .orElse(self.run.product(other.run).void)
    )

  /**
   * Symbolic alias for `or`
   *
   * @example
   *   {{{
   *     // success
   *     expect.eql("foo", "bar") ||
   *       expect.eql("alpha", "alpha")
   *   }}}
   *
   * @param other
   *   The second operand
   */
  def ||(other: Expectations): Expectations = or(other)

  /**
   * Logical XOR combinator for two `Expectations`
   *
   * @example
   *   {{{
   *     // failure
   *     expect.eql("foo", "foo") xor
   *       expect.eql("bar", "bar")
   *   }}}
   *
   * @param other
   *   The second operand
   */
  def xor(other: Expectations)(implicit loc: SourceLocation): Expectations =
    (run, other.run) match {
      case (Valid(_), Valid(_)) =>
        Expectations.Helpers.failure("Both expectations were met")
      case (otherL, otherR) =>
        Expectations(otherL.orElse(otherR).orElse(otherL.product(otherR).void))
    }

  def failFast[F[_]: Sync]: F[Unit] =
    this.run match {
      case Invalid(e) => Sync[F].raiseError(e.head)
      case Valid(_)   => Sync[F].unit
    }

  /**
   * Adds the specified location to the list of locations that will be reported
   * if an expectation is failed.
   */
  def traced(loc: SourceLocation): Expectations =
    Expectations(run.leftMap(_.map(e =>
      e.copy(locations = e.locations.append(loc)))))

}

object Expectations {

  /**
   * Trick to convert from multiplicative assertion to additive assertion
   */
  abstract class NewType { self =>
    type Base
    trait Tag extends Any
    type Type <: Base with Tag

    def apply(fa: Expectations): Type =
      fa.asInstanceOf[Type]

    def unwrap(fa: Type): Expectations =
      fa.asInstanceOf[Expectations]
  }

  object Additive extends NewType
  type Additive = Additive.Type

  implicit val multiplicativeMonoid: Monoid[Expectations] =
    new Monoid[Expectations] {
      override def empty: Expectations =
        Expectations(Validated.validNel(()))

      override def combine(x: Expectations, y: Expectations): Expectations =
        x.and(y)
    }

  implicit def additiveMonoid(
      implicit loc: SourceLocation): Monoid[Expectations.Additive] =
    new Monoid[Additive] {
      override def empty: Additive =
        Additive(
          Expectations(
            Validated.invalidNel(new AssertionException("empty",
                                                        NonEmptyList.of(loc)))))

      override def combine(x: Additive, y: Additive): Additive =
        Additive(
          Expectations(
            Additive.unwrap(x).or(Additive.unwrap(y)).run
          ))
    }

  trait Helpers {

    /**
     * Expect macros
     */
    def expect = new Expect
    def assert = new Expect

    val success: Expectations = Monoid[Expectations].empty

    def failure(hint: String)(implicit pos: SourceLocation): Expectations =
      Expectations(Validated.invalidNel(new AssertionException(
        hint,
        NonEmptyList.of(pos))))

    def succeed[A]: A => Expectations = _ => success

    def fail[A](hint: String)(implicit pos: SourceLocation): A => Expectations =
      _ => failure(hint)

    /**
     * Checks that an assertion is true for all elements in a foldable. Succeeds
     * if the foldable is empty.
     *
     * @example
     *   {{{
     *     val xs =
     *       List("foo", "bar")
     *
     *     // success
     *     forEach(xs)(s => expect.eql(3, s.length)
     *   }}}
     */
    def forEach[L[_], A](la: L[A])(f: A => Expectations)(
        implicit L: Foldable[L]): Expectations = la.foldMap(f)

    /**
     * Checks that an assertion is true for at least one element in a foldable.
     * Fails if the foldable is empty.
     *
     * @example
     *   {{{
     *     val xs =
     *       List("foo", "bar")
     *
     *     // success
     *     exists(xs)(s => expect.eql("foo", s)
     *   }}}
     */
    def exists[L[_], A](la: L[A])(f: A => Expectations)(
        implicit foldable: Foldable[L],
        pos: SourceLocation): Expectations =
      Expectations.Additive.unwrap(la.foldMap(a => Expectations.Additive(f(a))))

    /**
     * Checks that a given expression matches a certain pattern; fails
     * otherwise.
     *
     * @example
     *   {{{
     *     matches(Option(4)) { case Some(x) =>
     *       expect.eql(4, x)
     *     }
     *   }}}
     */
    def matches[A](x: A)(
        f: PartialFunction[A, Expectations]
    )(
        implicit pos: SourceLocation,
        A: Show[A] = Show.fromToString[A]): Expectations =
      if (f.isDefinedAt(x))
        f(x)
      else
        failure("Pattern did not match, got: " + x.show)

    /**
     * Alias for `forEach`
     */
    def inEach[L[_], A](la: L[A])(f: A => Expectations)(
        implicit L: Foldable[L]): Expectations = forEach(la)(f)

    /**
     * Checks that an `ApplicativeError` (like `Either`) is successful
     *
     * @example
     *   {{{
     *     val res: Either[String, Int] =
     *       Right(4)
     *
     *     whenSuccess(res) { n =>
     *       expect.eql(4, n)
     *     }
     *   }}}
     */
    def whenSuccess[F[_], A, E](fa: F[A])(
        f: A => Expectations
    )(
        implicit pos: SourceLocation,
        F: ApplicativeError[F, E],
        G: Foldable[F],
        E: Show[E] = Show.fromToString[E]): Expectations =
      fa
        .map(f)
        .handleError(e => failure("Expected success case, got: " + e.show))
        .foldLeft(failure(
          "unexpected error case encountered after error handling"))((z, x) =>
          z || x)

    def verify(condition: Boolean, hint: String)(
        implicit pos: SourceLocation): Expectations =
      if (condition) success
      else failure(hint)

    def verify(condition: Boolean)(
        implicit pos: SourceLocation): Expectations =
      verify(condition, "assertion failed!")

    def not(assertion: Expectations)(
        implicit pos: SourceLocation): Expectations = assertion.run match {
      case Valid(_)   => failure("Assertion was true")
      case Invalid(_) => success
    }

    implicit class StringOps(str: String) {
      def ignore(implicit loc: SourceLocation): TestName =
        new TestName(str, loc, Set.empty).ignore
      def only(implicit loc: SourceLocation): TestName =
        new TestName(str, loc, Set.empty).only
      def tagged(tag: String)(implicit loc: SourceLocation): TestName =
        new TestName(str, loc, Set.empty).tagged(tag)
    }

  }

  object Helpers extends Helpers

  def format(tpl: String, values: String*): String = {
    @scala.annotation.tailrec
    def loop(index: Int, acc: String): String =
      if (index >= values.length) acc
      else {
        val value = String.valueOf(values(index))
        val newStr =
          acc.replaceAll(s"[{]$index[}]",
                         java.util.regex.Matcher.quoteReplacement(value))
        loop(index + 1, newStr)
      }

    loop(0, tpl)
  }

}
