package weaver.ziocompat

import weaver.framework.DogFood
import weaver.ziocompat.modules._
import weaver.Log

import sbt.testing.Status
import zio._
import zio.duration._
import zio.interop.catz._

object ZIOSuiteTest extends ZIOSuite[KVStore with DogFoodz] {

  override def maxParallelism: Int = 1

  override val sharedLayer: ZLayer[ZEnv, Throwable, KVStore with DogFoodz] = {
    val kvstore: ZLayer[ZEnv, Throwable, KVStore] = ZLayer.fromEffect {
      Ref
        .make(Map.empty[String, String])
        .map(new KVStore.RefBased(_))
    }
    val dogfood: ZLayer[ZEnv, Throwable, DogFoodz] = {
      ZLayer.fromManaged(DogFood.make(new weaver.framework.ZIO()).toManagedZIO)
    }

    dogfood ++ kvstore
  }

  // Don't do this at home, kids
  test("setting some value in a shared store") {
    for {
      _          <- zio.clock.sleep(1.seconds)
      _          <- KVStore.put("hello", "world")
      helloValue <- KVStore.get("hello")
      _          <- log.info(helloValue.getOrElse("empty"))
    } yield expect(List(1, 2, 3).size == 3)
  }

  test("getting the value set in a previous test") {
    for {
      previous <- KVStore.delete("hello")
      now      <- KVStore.get("hello")
    } yield expect(previous == Some("world")) and expect(now == None)
  }

  List(
    TestWithExceptionInTest,
    TestWithExceptionInExpectation,
    TestWithExceptionInInitialisation,
    TestWithEventualDiedSharedLayer,
    TestWithFailedSharedLayer
  ).foreach { testSuite =>
    test(s"fail properly in ${testSuite.getClass.getSimpleName}") {
      for {
        (_, events) <- DogFoodz.runSuite(testSuite)
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

  test("fail properly on failed expectations") {
    for {
      (_, events) <- DogFoodz.runSuite(TestWithFailedExpectation)
    } yield {
      val maybeEvent  = events.headOption
      val maybeStatus = maybeEvent.map(_.status())
      expect(maybeStatus.contains(Status.Failure))
    }
  }

  test("logs contain only the logs from each test") {
    FiberRefLogTest.spec(List.empty)
      .map(outcome =>
        expect(!outcome.status.isFailed) and expect(outcome.log.size == 1))
      .compile
      .foldMonoid
  }

  test("logs can use adapter to give logs from app") {
    LogAdapterTest.spec(List.empty).map(outcome =>
      expect(!outcome.status.isFailed) and
        expect(outcome.log.map(_.msg) == cats.data.Chain(
          "one",
          "two",
          "three",
          "four"))).compile.foldMonoid
  }

  object LogAdapterTest extends ZIOSuite[Has[SomeApp.Service]] {

    val loggingAdapterLayer: RLayer[LogModule, Has[SomeLogger.Service]] =
      ZLayer.fromService[LogModule.Service, SomeLogger.Service] {
        weaverLogger =>
          new SomeLogger.Service {
            override def log(msg: String): Task[Unit] =
              weaverLogger.log(Log.Entry(
                0L,
                msg,
                Map.empty,
                Log.debug,
                None,
                weaver.SourceLocation("", "", 0)))
          }
      }

    val sharedLayer: ZLayer[ZEnv with LogModule, Throwable, Has[SomeApp.Service]] =
      loggingAdapterLayer >>> SomeApp.program

    test("can run and log") {
      for {
        _    <- SomeApp.run()
        logs <- LogModule.logs
      } yield expect(logs.size == 4)
    }

  }

  object SomeLogger {
    trait Service {
      def log(msg: String): Task[Unit]
    }
    def log(msg: String): RIO[Has[Service], Unit] =
      ZIO.accessM[Has[Service]](_.get.log(msg))
  }

  object SomeApp {
    class Program(logger: SomeLogger.Service) extends SomeApp.Service {
      def run(): Task[Unit] = for {
        _ <- logger.log("one")
        _ <- logger.log("two")
        _ <- logger.log("three")
        _ <- logger.log("four")
      } yield ()
    }

    val program: URLayer[Has[SomeLogger.Service], Has[SomeApp.Service]] =
      ZLayer.fromService[SomeLogger.Service, SomeApp.Service](new Program(_))

    trait Service {
      def run(): Task[Unit]
    }
    def run(): RIO[Has[Service], Unit] =
      ZIO.accessM[Has[Service]](_.get.run())
  }

  object FiberRefLogTest extends SimpleZIOSuite {
    override def maxParallelism: Int = 1
    test("debug log") {
      (log.debug("a log") *> LogModule.logs).map(logs => expect(logs.size == 1))
    }

    test("error log") {
      (log.error("a log") *> LogModule.logs).map(logs => expect(logs.size == 1))
    }

    test("warning log") {
      (log.info("a log") *> LogModule.logs).map(logs => expect(logs.size == 1))
    }

    test("warning log") {
      (log.warn("a log") *> LogModule.logs).map(logs => expect(logs.size == 1))
    }
  }

  object TestWithExceptionInTest extends SimpleZIOSuite {
    test("example test") {
      Task.fail(new RuntimeException("oh no"))
    }
  }

  object TestWithExceptionInExpectation extends SimpleZIOSuite {
    test("example test") {
      for {
        _ <- Task.succeed(())
      } yield throw new RuntimeException("oh no")
    }
  }

  object TestWithExceptionInInitialisation extends SimpleZIOSuite {
    test("example test") {
      throw new RuntimeException("oh no")
    }
  }

  object TestWithFailedExpectation extends SimpleZIOSuite {
    test("example test") {
      for {
        _ <- Task.succeed(())
      } yield expect(false)
    }
  }

  object TestWithFailedSharedLayer extends MutableZIOSuite[Has[Unit]] {
    override val sharedLayer: ZLayer[zio.ZEnv, Throwable, Has[Unit]] =
      ZLayer.fail(new RuntimeException("oh no"))

    test("example test") {
      ZIO.succeed(expect(true))
    }
  }

  object TestWithEventualDiedSharedLayer extends MutableZIOSuite[Has[Unit]] {
    override val sharedLayer: ZLayer[zio.ZEnv, Throwable, Has[Unit]] =
      ZLayer.fromEffect(ZIO.effect(throw new RuntimeException("oh no")))

    test("example test") {
      ZIO.succeed(expect(true))
    }
  }
}

object modules {

  type DogFoodz = Has[DogFood[T]]
  type KVStore  = Has[KVStore.Service]

  object DogFoodz {
    def runSuite(suite: BaseZIOSuite): RIO[ZEnv with DogFoodz, DogFood.State] =
      ZIO.accessM(_.get[DogFood[T]].runSuite(suite))
  }

  object KVStore {

    // boileplate usually written via macro
    def put(k: String, v: String): RIO[KVStore, Unit] =
      ZIO.accessM(_.get.put(k, v))
    def get(k: String): RIO[KVStore, Option[String]] =
      ZIO.accessM(_.get.get(k))
    def delete(k: String): RIO[KVStore, Option[String]] =
      ZIO.accessM(_.get.delete(k))

    trait Service {
      def put(k: String, v: String): UIO[Unit]
      def get(k: String): UIO[Option[String]]
      def delete(k: String): UIO[Option[String]]
    }

    class RefBased(ref: Ref[Map[String, String]]) extends Service {
      def put(k: String, v: String): zio.UIO[Unit] = ref.update(_ + (k -> v))

      def get(k: String): zio.UIO[Option[String]] = ref.get.map(_.get(k))

      def delete(k: String): zio.UIO[Option[String]] =
        ref.getAndUpdate(_ - k).map(_.get(k))

    }

  }

}
