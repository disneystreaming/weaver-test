package weaver
package scalacheck

import scala.concurrent.duration._

import cats.effect.IO
import cats.syntax.all._

import weaver.framework._

object PropertyDogFoodTest extends SimpleIOSuite with DogFood {

  test("Failed property tests get reported properly") {
    for {
      (logs, events) <- runSuite(Meta.FailedChecks)
    } yield {
      val errorLogs = logs.collect {
        case LoggedEvent.Error(msg) => msg
      }
      exists(errorLogs) { log =>
        val expectedMessage = "Property test failed on try 5 with input 0"
        expect(log.contains(expectedMessage))
      }
    }
  }

  // 100 checks sleeping 1 second each should not take 100 seconds
  simpleTest("Checks are parallelised") {
    for {
      (_, events) <- runSuite(Meta.ParallelChecks)
      _           <- expect(events.size == 1).failFast
    } yield {
      expect(events.headOption.get.duration() < 10000)
    }
  }

}

object Meta {
  object ParallelChecks extends SimpleIOSuite with IOCheckers {

    override def checkConfig: CheckConfig =
      super.checkConfig
        .copy(perPropertyParallelism = 100, minimumSuccessful = 100)

    simpleTest("sleeping forall") {
      forall { (x: Int, y: Int) =>
        IO.sleep(1.second) *> IO(expect(x + y == y + x))
      }
    }
  }

  object FailedChecks extends SimpleIOSuite with IOCheckers {

    override def checkConfig: CheckConfig =
      super.checkConfig.copy(perPropertyParallelism = 1, initialSeed = Some(1L))

    simpleTest("foobar") {
      forall { x: Int =>
        expect(x > 0)
      }
    }
  }
}
