package weaver.discipline

import weaver.SimpleIOSuite
import org.typelevel.discipline.Laws
import org.scalacheck.Arbitrary
import cats.laws.discipline._
import cats.kernel.Eq

object IntegrationTest extends SimpleIOSuite {

  test("Runs tests successfully") {
    MetaSuccess.spec(Nil).compile.toList.map { outcomes =>
      expect.all(
        outcomes.map(_.name).toSet == Set("Int: rickroll.rolls",
                                          "Int: rickroll.ricks"),
        outcomes.map(_.status.isFailed).forall(_ == false)
      )
    }
  }

  test("Reports failures correctly") {
    MetaFailure.spec(Nil).compile.toList.map { outcomes =>
      expect.all(
        outcomes.map(_.name).toSet == Set("Boolean: rickroll.rolls",
                                          "Boolean: rickroll.ricks"),
        outcomes.map(_.status.isFailed).count(_ == true) == 1,
        outcomes.find(_.status.isFailed).exists { to =>
          to.name == "Boolean: rickroll.ricks"
        }
      )
    }
  }

  object MetaSuccess extends Discipline {
    checkAll("Int", RickrollTests[Int].all)
  }

  object MetaFailure extends Discipline {
    checkAll("Boolean", RickrollTests[Boolean].all)
  }
}

trait RickRoll[A] {
  def rick(a: A): A
  def roll(a: A): Option[A]
}

object RickRoll {
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

