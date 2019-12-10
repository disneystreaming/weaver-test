package weaver

import cats._
import cats.implicits._
import cats.data.{ Validated, ValidatedNel }
import cats.data.Validated._

case class Expectations(val run: ValidatedNel[AssertionException, Unit]) {
  self =>

  def and(other: Expectations) =
    Expectations(self.run.product(other.run).void)

  def or(other: Expectations) =
    Expectations(
      self.run
        .orElse(other.run)
        .orElse(self.run.product(other.run).void)
    )

}

object Expectations {

  implicit def fromSingle(e: SingleExpectation)(
      implicit loc: SourceLocation): Expectations =
    Expectations(e.run.leftMap(_.map(str => new AssertionException(str, loc))))

  /**
   * Trick to convert from multiplicative assertion to additive assertion
   *
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
            Validated.invalidNel(new AssertionException("empty", loc))))

      override def combine(x: Additive, y: Additive): Additive =
        Additive(
          Expectations(
            Additive.unwrap(x).or(Additive.unwrap(y)).run
          ))
    }

  trait Helpers {

    val success: Expectations = Monoid[Expectations].empty

    def failure(hint: String)(implicit pos: SourceLocation): Expectations =
      Expectations(Validated.invalidNel(new AssertionException(hint, pos)))

    def succeed[A]: A => Expectations = _ => success

    def fail[A](hint: String)(implicit pos: SourceLocation): A => Expectations =
      _ => failure(hint)

    /**
     * Checks that an assertion is true for all elements in a foldable.
     * Succeeds if the foldable is empty.
     */
    def forall[L[_], A](la: L[A])(f: A => Expectations)(
        implicit L: Foldable[L]): Expectations = la.foldMap(f)

    /**
     * Checks that an assertion is true for at least one element in a foldable.
     * Fails if the foldable is empty.
     */
    def exists[L[_], A](la: L[A])(f: A => Expectations)(
        implicit foldable: Foldable[L],
        pos: SourceLocation): Expectations =
      Expectations.Additive.unwrap(la.foldMap(a => Expectations.Additive(f(a))))

    /**
     * Alias to forall
     */
    def inEach[L[_], A](la: L[A])(f: A => Expectations)(
        implicit L: Foldable[L]): Expectations = forall(la)(f)

    def verify(condition: Boolean, hint: String)(
        implicit
        pos: SourceLocation): Expectations =
      if (condition) success
      else failure(hint)

    def verify(condition: Boolean)(
        implicit
        pos: SourceLocation): Expectations =
      verify(condition, "assertion failed!")

    def not(assertion: Expectations)(
        implicit
        pos: SourceLocation): Expectations = assertion.run match {
      case Valid(_)   => failure("Assertion was true")
      case Invalid(_) => success
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
