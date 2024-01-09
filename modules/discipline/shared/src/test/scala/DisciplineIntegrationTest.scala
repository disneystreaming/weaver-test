package weaver.discipline

import weaver.SimpleIOSuite

object DisciplineIntegrationTest extends SimpleIOSuite {

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

  test("Captures exceptions correctly") {
    import RickRoll._
    MetaException.spec(Nil).compile.toList.map { outcomes =>
      expect.all(
        outcomes.forall(_.cause.isDefined),
        outcomes.flatMap(_.cause).collect {
          case `oops` | `oops2` => true
          case _                => false
        }.size == 2
      )
    }
  }

  object MetaSuccess extends weaver.FunSuite with Discipline {
    checkAll("Int", RickrollTests[Int].all)
  }

  object MetaFailure extends weaver.FunSuite with Discipline {
    checkAll("Boolean", RickrollTests[Boolean].all)
  }

  object MetaException extends weaver.FunSuite with Discipline {
    checkAll("String", RickrollTests[String].all)
  }
}
