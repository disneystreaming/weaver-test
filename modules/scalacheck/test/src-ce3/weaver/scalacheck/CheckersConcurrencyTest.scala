package weaver
package scalacheck

import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration._

import cats.effect.Outcome._
import cats.effect.testkit.TestControl
import cats.effect.{ IO, Ref }
import cats.syntax.all._

import weaver.TestStatus

import org.scalacheck.Gen

object CheckersConcurrencyTest extends SimpleIOSuite {
  test("tests should wait for slower tests to succeed before completion") {

    val minTests = 10
    object CheckersConcurrencyTestNested extends SimpleIOSuite with Checkers {

      override def checkConfig: CheckConfig =
        super.checkConfig.copy(perPropertyParallelism = minTests * 5,
                               minimumSuccessful = minTests,
                               maximumDiscardRatio = 10)

      loggedTest("nested") { log =>
        val atomicInt = new AtomicInteger(0)
        forall(Gen.delay(Gen.const(atomicInt.incrementAndGet()))) { i =>
          val sleepFor =
            Duration.create(math.max(0L, minTests - i.toLong), TimeUnit.SECONDS)
          IO.sleep(sleepFor) *> log.info(s"Ran gen $i").as(success)
        }
      }
    }

    val expectedLogs = (1 to minTests).map(i => s"Ran gen $i").toList
    TestControl.execute(runSuite(CheckersConcurrencyTestNested))
      .flatMap { control =>
        control.advanceAndTick(1.second).replicateA(minTests) *> control.results
      }
      .map {
        case Some(Succeeded(r)) =>
          expect(r.count(_.status == TestStatus.Success) == 1) and
            expect(r.forall(_.status == TestStatus.Success)) and
            expect.same(
              expectedLogs,
              expectedLogs.intersect(r.flatMap(_.log.toList.map(_.msg)))
            )
        case Some(Errored(e)) => failure(e.toString)
        case Some(Canceled()) => failure("property test was cancelled")
        case None             => failure("property test didn't complete")
      }
  }

  test("tests should wait for slower tests to fail before completion") {

    object CheckersConcurrencyTestNested extends SimpleIOSuite with Checkers {

      override def checkConfig: CheckConfig =
        super.checkConfig.copy(perPropertyParallelism = 50,
                               minimumSuccessful = 10,
                               maximumDiscardRatio = 10)

      test("nested") {
        val atomicInt = new AtomicInteger(0)
        forall(Gen.delay(Gen.const(atomicInt.incrementAndGet()))) { i =>
          if (i == 1) IO.sleep(1.second).as(failure("first one fails"))
          else IO.pure(success)
        }
      }
    }

    TestControl.execute(runSuite(CheckersConcurrencyTestNested))
      .flatMap { control =>
        control.tick *> control.advanceAndTick(1.second) *> control.results
      }
      .map {
        case Some(Succeeded(r)) =>
          expect(r.count(_.status == TestStatus.Failure) == 1) and
            expect(r.forall(_.status == TestStatus.Failure))
        case Some(Errored(e)) => failure(e.toString)
        case Some(Canceled()) => failure("property test was cancelled")
        case None             => failure("property test didn't complete")
      }
  }

  private def runSuite(suite: SimpleIOSuite) = for {
    ref <- Ref.of(List.empty[TestOutcome])
    _ <- suite.run(Nil)(result =>
      ref.update(result :: _))
    results <- ref.get
  } yield results
}
