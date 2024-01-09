package weaver.discipline

import cats.kernel.Eq
import cats.laws.discipline._

import org.scalacheck.Arbitrary
import org.typelevel.discipline.Laws

trait RickRoll[A] {
  def rick(a: A): A
  def roll(a: A): Option[A]
}

object RickRoll {
  case object oops  extends Exception
  case object oops2 extends Exception
  // Int is a well behaving rickroll type
  implicit val rr: RickRoll[Int] = new RickRoll[Int] {
    def rick(a: Int): Int = a

    def roll(a: Int): Option[Int] = Some(a)
  }

  // Boolean is an unlawful rickroll type
  implicit val rrB: RickRoll[Boolean] = new RickRoll[Boolean] {
    def rick(a: Boolean): Boolean = !a // stop! in the name of Law

    def roll(a: Boolean): Option[Boolean] = Some(a)
  }

  // String is an unlawful rickroll type
  implicit val rrS: RickRoll[String] = new RickRoll[String] {
    def rick(a: String): String = throw oops

    def roll(a: String): Option[String] = throw oops2
  }
}

trait RickrollTests[A] extends Laws {
  import org.scalacheck.Prop.forAll
  import cats.kernel.laws._

  implicit def R: RickRoll[A]

  def rollLaw(x: A): IsEq[Boolean] =
    R.roll(x).isDefined <-> true

  def rickLaw(x: A)(implicit eq: Eq[A]): IsEq[Boolean] =
    eq.eqv(R.rick(x), x) <-> true

  def all(implicit arbA: Arbitrary[A], eqA: Eq[A]): RuleSet = {
    new DefaultRuleSet(
      "rickroll",
      None,
      "rolls" -> forAll(rollLaw _),
      "ricks" -> forAll(rickLaw _)
    )
  }
}

object RickrollTests {
  def apply[A](implicit rr: RickRoll[A]): RickrollTests[A] =
    new RickrollTests[A] { override implicit def R = rr }
}
