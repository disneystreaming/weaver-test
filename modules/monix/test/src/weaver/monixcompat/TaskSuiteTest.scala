package weaver.monixcompat

import weaver.framework.DogFood

import monix.eval.Task
import sbt.testing.Status

object TaskSuiteTest extends SimpleTaskSuite {
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
