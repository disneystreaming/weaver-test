package weaver.monixbiocompat

import weaver.framework.DogFood

import monix.bio.Task
import sbt.testing.Status

object IOSuiteTest extends SimpleIOSuite {
  List(
    TestWithExceptionInTest,
    TestWithExceptionInExpectation,
    TestWithExceptionInInitialisation
  ).foreach { testSuite =>
    test(s"fail properly in ${testSuite.getClass.getSimpleName}") {
      for {
        (_, events) <- DogFood.runSuite(testSuite).to[Task]
      } yield {
        val maybeEvent = events.headOption
        val maybeThrowable = maybeEvent.flatMap { event =>
          if (event.throwable().isDefined()) Some(event.throwable().get())
          else None
        }
        val maybeStatus = maybeEvent.map(_.status())
        expect(maybeStatus.contains(Status.Error)) &&
        expect(maybeThrowable.map(_.getMessage).contains("oh no"))
      }
    }
  }

  test("fail properly on failed expectations") { _ =>
    for {
      (_, events) <- DogFood.runSuite(TestWithFailedExpectation).to[Task]
    } yield {
      val maybeEvent  = events.headOption
      val maybeStatus = maybeEvent.map(_.status())
      expect(maybeStatus.contains(Status.Failure))
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
