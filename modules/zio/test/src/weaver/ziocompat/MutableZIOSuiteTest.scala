package weaver.ziocompat

import sbt.testing.Status
import weaver.framework.DogFood
import weaver.ziocompat.modules._
import zio._
import zio.duration._

object ZIOSuiteTest extends ZIOSuite[KVStore] {

  override def maxParallelism: Int = 1

  override val sharedLayer = ZLayer.fromEffect {
    Ref
      .make(Map.empty[String, String])
      .map(new KVStore.RefBased(_))
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
        (_, events) <- DogFood.runSuite(testSuite).to[Task]
      } yield {
        val event = events.headOption.get
        expect(event.status == Status.Error) and
          expect(event.throwable().get().getMessage == "oh no")
      }
    }
  }

  test("fail properly on failed expectations") {
    for {
      (_, events) <- DogFood.runSuite(TestWithFailedExpectation).to[Task]
    } yield expect(events.headOption.get.status == Status.Failure)
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

  type KVStore = Has[KVStore.Service]
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
