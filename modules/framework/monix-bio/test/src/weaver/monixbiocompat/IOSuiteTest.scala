package weaver.monixbiocompat

import cats.effect.Resource

import weaver.framework.{ DogFood, MonixBIO }

import monix.bio.Task
import sbt.testing.Status.{ Error, Failure }

object IOSuiteTest extends MutableIOSuite {
  override type Res = DogFood[Task]
  override def sharedResource: Resource[monix.bio.Task, Res] =
    DogFood.make(new MonixBIO)

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
        expect(maybeStatus.contains(Error)) &&
        expect(maybeThrowable.map(_.getMessage).contains("oh no"))
      }
    }
  }

  test("fail properly on failed expectations") { dogfood =>
    dogfood.runSuite(TestWithFailedExpectation).map { case (_, events) =>
      val maybeEvent  = events.headOption
      val maybeStatus = maybeEvent.map(_.status())
      expect(maybeStatus.contains(Failure))
    }
  }

  object TestWithExceptionInTest extends SimpleIOSuite {
    test("example test") {
      Task.raiseError(new RuntimeException("oh no"))
    }
  }

  object TestWithExceptionInExpectation extends SimpleIOSuite {
    test("example test") {
      for {
        _ <- Task.unit
      } yield throw new RuntimeException("oh no")
    }
  }

  object TestWithExceptionInInitialisation extends SimpleIOSuite {
    test("example test") { _ =>
      throw new RuntimeException("oh no")
    }
  }

  object TestWithFailedExpectation extends SimpleIOSuite {
    test("example test") { _ =>
      for {
        _ <- Task.unit
      } yield expect(false)
    }
  }
}
