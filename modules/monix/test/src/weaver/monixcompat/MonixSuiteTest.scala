package weaver.monixcompat

import cats.effect.Resource

import weaver.framework.DogFood

import monix.eval.Task
import sbt.testing.Status

object MonixSuiteTest extends MonixSuite {
  type Res = Unit
  override def sharedResource: Resource[Task, Unit] = Resource.pure(())

  List(
    TestWithExceptionInTest,
    TestWithExceptionInExpectation,
    TestWithExceptionInInitialisation
  ).foreach { testSuite =>
    test(s"fail properly in ${testSuite.getClass.getSimpleName}") {
      for {
        (_, events) <- DogFood.runSuite(testSuite).to[Task]
      } yield {
        val event = events.headOption.get
        expect(event.status() == Status.Error) and
          expect(event.throwable().get().getMessage == "oh no")
      }
    }
  }

  test("fail properly on failed expectations") { _ =>
    for {
      (_, events) <- DogFood.runSuite(TestWithFailedExpectation).to[Task]
    } yield expect(events.headOption.get.status() == Status.Failure)
  }

  object TestWithExceptionInTest extends SimpleMonixSuite {
    test("example test") {
      Task.raiseError(new RuntimeException("oh no"))
    }
  }

  object TestWithExceptionInExpectation extends SimpleMonixSuite {
    test("example test") {
      for {
        _ <- Task.unit
      } yield throw new RuntimeException("oh no")
    }
  }

  object TestWithExceptionInInitialisation extends SimpleMonixSuite {
    test("example test") { _ =>
      throw new RuntimeException("oh no")
    }
  }

  object TestWithFailedExpectation extends SimpleMonixSuite {
    test("example test") { _ =>
      for {
        _ <- Task.unit
      } yield expect(false)
    }
  }
}
