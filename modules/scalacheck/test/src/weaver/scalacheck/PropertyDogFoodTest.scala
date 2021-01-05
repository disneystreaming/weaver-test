package weaver
package scalacheck

import scala.concurrent.duration._

import cats.effect.{ IO, Resource }
import cats.syntax.all._

import weaver.framework._

object PropertyDogFoodTest extends IOSuite {

  type Res = DogFood[IO]
  def sharedResource: Resource[IO, DogFood[IO]] =
    DogFood.make(new CatsEffect)

  test("Failed property tests get reported properly") { dogfood =>
    for {
      results <- dogfood.runSuite(Meta.FailedChecks)
      logs   = results._1
    } yield {
      val errorLogs = logs.collect {
        case LoggedEvent.Error(msg) => msg
      }
      exists(errorLogs) { log =>
        // Go into software engineering they say
        // Learn how to make amazing algorithms
        // Build robust and deterministic software
        val (attempt, value) =
          if (ScalaCompat.isScala3)
            ("4", "-2147483648")
          else
            ("2", "0")

        val expectedMessage =
          s"Property test failed on try $attempt with input $value"
        expect(log.contains(expectedMessage))
      }
    }
  }

  // 100 checks sleeping 1 second each should not take 100 seconds
  test("Checks are parallelised") { dogfood =>
    for {
      events <- dogfood.runSuite(Meta.ParallelChecks).map(_._2)
      _      <- expect(events.size == 1).failFast
    } yield {
      expect(events.headOption.get.duration() < 10000)
    }
  }

}

object Meta {
  object ParallelChecks extends SimpleIOSuite with Checkers {

    override def checkConfig: CheckConfig =
      super.checkConfig
        .copy(perPropertyParallelism = 100, minimumSuccessful = 100)

    simpleTest("sleeping forall") {
      forall { (x: Int, y: Int) =>
        IO.sleep(1.second) *> IO(expect(x + y == y + x))
      }
    }
  }

  object FailedChecks extends SimpleIOSuite with Checkers {

    override def checkConfig: CheckConfig =
      super.checkConfig.copy(perPropertyParallelism = 1, initialSeed = Some(5L))

    simpleTest("foobar") {
      forall { (x: Int) =>
        expect(x > 0)
      }
    }
  }
}
