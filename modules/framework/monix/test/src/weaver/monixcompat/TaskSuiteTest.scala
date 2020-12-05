package weaver.monixcompat

import cats.effect.Resource

import weaver.framework.{DogFood, Monix}

import monix.eval.Task
import sbt.testing.Status

object TaskSuiteTest extends MutableTaskSuite {

  type Res = DogFood[Task]
  def sharedResource: Resource[Task, DogFood[Task]] =
    DogFood.make(new Monix)

  List(
    TestWithExceptionInTest,
    TestWithExceptionInExpectation,
    TestWithExceptionInInitialisation
  ).foreach { testSuite =>
    test(s"fail properly in ${testSuite.getClass.getSimpleName}") { dogfood =>
      dogfood.runSuite(testSuite).map { case (_, events) =>
        val maybeEvent = events.headOption
        val maybeThrowable = maybeEvent.flatMap { event =>
          if (event.throwable().isDefined()) Some(event.throwable().get())
          else None
        }
        val maybeStatus = maybeEvent.map(_.status())

        expect.all(
          maybeStatus.contains(Status.Error),
          maybeThrowable.map(_.getMessage).contains("oh no")
        )

      }
    }
  }

  test("fail properly on failed expectations") { dogfood =>
    dogfood.runSuite(TestWithFailedExpectation).map { case (_, events) =>
      val maybeEvent  = events.headOption
      val maybeStatus = maybeEvent.map(_.status())
      expect(maybeStatus.contains(Status.Failure))
    }
  }

  object TestWithExceptionInTest extends SimpleTaskSuite {
    test("example test") {
      Task.raiseError(new RuntimeException("oh no"))
    }
  }

  object TestWithExceptionInExpectation extends SimpleTaskSuite {
    test("example test") {
      for {
        _ <- Task.unit
      } yield throw new RuntimeException("oh no")
    }
  }

  object TestWithExceptionInInitialisation extends SimpleTaskSuite {
    test("example test") { _ =>
      throw new RuntimeException("oh no")
    }
  }

  object TestWithFailedExpectation extends SimpleTaskSuite {
    test("example test") { _ =>
      for {
        _ <- Task.unit
      } yield expect(false)
    }
  }
}
