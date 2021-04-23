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
      logs = results._1
    } yield {
      val errorLogs = logs.collect {
        case LoggedEvent.Error(msg) => msg
      }
      exists(errorLogs) { log =>
        // Go into software engineering they say
        // Learn how to make amazing algorithms
        // Build robust and deterministic software
        val (attempt, value, seed) =
          if (ScalaCompat.isScala3) {
            ("4",
             "-2147483648",
             """Seed.fromBase64("AkTFK0oQzv-BOkf-rqnsdb_Etapzkj9gQD9rHj7UnKM=")""")
          } else {
            ("2",
             "0",
             """Seed.fromBase64("Nj62qCHF96VYEMGcD2OBlfmuyihbPQQhQLH9acYL5RA=")""")
          }

        val expectedMessage =
          s"Property test failed on try $attempt with seed $seed and input $value"

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

  // 5 checks with perPropertyParallelism = 1 sleeping 1 second each should take at least 5 seconds
  test("Config can be overridden") { dogfood =>
    for {
      events <- dogfood.runSuite(Meta.ConfigOverrideChecks).map(_._2)
      _      <- expect(events.size == 1).failFast
    } yield {
      expect(events.headOption.get.duration() >= 5000)
    }
  }
}

object Meta {
  trait ParallelChecks extends SimpleIOSuite with Checkers {

    def partiallyAppliedForall: PartiallyAppliedForall

    test("sleeping forall") {
      partiallyAppliedForall { (x: Int, y: Int) =>
        IO.sleep(1.second) *> IO(expect(x + y == y + x))
      }
    }
  }

  object ParallelChecks extends ParallelChecks {

    override def partiallyAppliedForall: PartiallyAppliedForall = forall

    override def checkConfig: CheckConfig =
      super.checkConfig
        .copy(perPropertyParallelism = 100, minimumSuccessful = 100)
  }

  object FailedChecks extends SimpleIOSuite with Checkers {

    override def checkConfig: CheckConfig =
      super.checkConfig.copy(perPropertyParallelism = 1, initialSeed = Some(5L))

    test("foobar") {
      forall { (x: Int) =>
        expect(x > 0)
      }
    }
  }

  object ConfigOverrideChecks extends ParallelChecks {

    override def partiallyAppliedForall: PartiallyAppliedForall =
      forall.withConfig(super.checkConfig.copy(
        perPropertyParallelism = 1,
        minimumSuccessful = 5))
  }
}
