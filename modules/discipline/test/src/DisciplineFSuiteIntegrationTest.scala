package weaver.discipline

import cats.effect.{ IO, Resource }

import weaver.{ BaseIOSuite, SimpleIOSuite }

object DisciplineFSuiteIntegrationTest extends SimpleIOSuite {

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

  test("Shared resource fails to start") {
    FailingResource.spec(Nil).compile.toList.attempt.map { outcomes =>
      expect.all(
        outcomes.isLeft,
        outcomes.left.exists(_ == resourceStart)
      )
    }
  }

  object MetaSuccess extends DisciplineFSuite[IO] with BaseIOSuite {
    override type Res = String
    override def sharedResource: Resource[IO, String] =
      Resource.pure("resource")

    checkAll("Int").pure(_ => RickrollTests[Int].all)
  }

  object MetaFailure extends DisciplineFSuite[IO] with BaseIOSuite {
    override type Res = String
    override def sharedResource: Resource[IO, String] =
      Resource.pure("resource")

    checkAll("Boolean").pure(_ => RickrollTests[Boolean].all)
  }

  object MetaException extends DisciplineFSuite[IO] with BaseIOSuite {
    override type Res = String
    override def sharedResource: Resource[IO, String] =
      Resource.pure("resource")

    checkAll("String").pure(_ => RickrollTests[String].all)
  }

  object resourceStart extends Exception
  object FailingResource extends DisciplineFSuite[IO] with BaseIOSuite {
    override type Res = String
    override def sharedResource: Resource[IO, String] =
      Resource.eval(IO.raiseError(resourceStart))

    checkAll("Int").pure(_ => RickrollTests[Int].all)
  }

}
