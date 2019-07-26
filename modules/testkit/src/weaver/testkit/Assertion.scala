package weaver
package testkit

import cats._
import cats.implicits._
import cats.data.{ Validated, ValidatedNel }
import cats.data.Validated._

case class Assertion(val run: ValidatedNel[AssertionException, Unit]) { self =>

  def and(other: Assertion) =
    Assertion(self.run.product(other.run).void)

  def or(other: Assertion) =
    Assertion(
      self.run
        .orElse(other.run)
        .orElse(self.run.product(other.run).void)
    )

}

object Assertion {

  /**
   * Trick to convert from multiplicative assertion to additive assertion
   *
   */
  abstract class NewType { self =>
    type Base
    trait Tag extends Any
    type Type <: Base with Tag

    def apply(fa: Assertion): Type =
      fa.asInstanceOf[Type]

    def unwrap(fa: Type): Assertion =
      fa.asInstanceOf[Assertion]
  }

  object Additive extends NewType
  type Additive = Additive.Type

  implicit val multiplicativeMonoid: Monoid[Assertion] =
    new Monoid[Assertion] {
      override def empty: Assertion =
        Assertion(Validated.validNel(()))

      override def combine(x: Assertion, y: Assertion): Assertion =
        x.and(y)
    }

  implicit def additiveMonoid(
      implicit loc: SourceLocation): Monoid[Assertion.Additive] =
    new Monoid[Additive] {
      override def empty: Additive =
        Additive(
          Assertion(Validated.invalidNel(new AssertionException("empty", loc))))

      override def combine(x: Additive, y: Additive): Additive =
        Additive(
          Assertion(
            Additive.unwrap(x).or(Additive.unwrap(y)).run
          ))
    }

  trait Helpers {

    val success: Assertion = Monoid[Assertion].empty

    def failure(hint: String)(implicit pos: SourceLocation): Assertion =
      Assertion(Validated.invalidNel(new AssertionException(hint, pos)))

    def succeed[A]: A => Assertion = _ => success

    def fail[A](hint: String)(implicit pos: SourceLocation): A => Assertion =
      _ => failure(hint)

    /**
     * Checks that an assertion is true for all elements in a foldable.
     * Succeeds if the foldable is empty.
     */
    def forall[L[_], A](la: L[A])(f: A => Assertion)(
        implicit L: Foldable[L]): Assertion = la.foldMap(f)

    /**
     * Checks that an assertion is true for at least one element in a foldable.
     * Fails if the foldable is empty.
     */
    def exists[L[_], A](la: L[A])(f: A => Assertion)(
        implicit foldable: Foldable[L],
        pos: SourceLocation): Assertion =
      Assertion.Additive.unwrap(la.foldMap(a => Assertion.Additive(f(a))))

    /**
     * Alias to forall
     */
    def inEach[L[_], A](la: L[A])(f: A => Assertion)(
        implicit L: Foldable[L]): Assertion = forall(la)(f)

    def verify(condition: Boolean, hint: String)(
        implicit
        pos: SourceLocation): Assertion =
      if (condition) success
      else failure(hint)

    def verify(condition: Boolean)(
        implicit
        pos: SourceLocation): Assertion =
      verify(condition, "assertion failed!")

    def not(assertion: Assertion)(
        implicit
        pos: SourceLocation): Assertion = assertion.run match {
      case Valid(_)   => failure("Assertion was true")
      case Invalid(_) => success
    }

    implicit def toComparison[T](t: T): ComparisonOps[T] =
      new ComparisonOps[T](t)
  }

  object Helpers extends Helpers

  class ComparisonOps[T](val t: T) extends AnyVal {
    import Helpers.{ success, failure }

    def must_==(other: T)(
        implicit E: Eq[T],
        S: Show[T],
        pos: SourceLocation): Assertion =
      if (t === other) success
      else
        failure(format("received {0} != expected {1}", t.show, other.show))

    def must_<=(other: T)(
        implicit O: PartialOrder[T],
        S: Show[T],
        pos: SourceLocation): Assertion =
      if (t <= other) success
      else
        failure(format("{0} was not <= {1}", t.show, other.show))

    def must_<(other: T)(
        implicit O: PartialOrder[T],
        S: Show[T],
        pos: SourceLocation): Assertion =
      if (t < other) success
      else
        failure(format("{0} was not < {1}", t.show, other.show))

    def must_>(other: T)(
        implicit O: PartialOrder[T],
        S: Show[T],
        pos: SourceLocation): Assertion =
      if (t > other) success
      else
        failure(format("{0} was not > {1}", t.show, other.show))

    def must_>=(other: T)(
        implicit O: PartialOrder[T],
        S: Show[T],
        pos: SourceLocation): Assertion =
      if (t >= other) success
      else
        failure(format("{0} was not >= {1}", t.show, other.show))

  }

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
