package weaver
package framework
package test

import java.io.File

import scala.concurrent.duration._

import cats.effect._

// The build tool will only detect and run top-level test suites. We can however nest objects
// that contain failing tests, to allow for testing the framework without failing the build
// because the framework will have ran the tests on its own.
object MetaJVM {

  object MutableSuiteTest extends MutableSuiteTest

  object GlobalStub extends GlobalResource {
    def sharedResources(store: GlobalWrite): Resource[IO, Unit] =
      Resource.make(makeTmpFile)(deleteFile).flatMap { file =>
        store.putR(file)
      }

    val makeTmpFile = IO(java.io.File.createTempFile("hello", ".tmp"))
    def deleteFile(file: java.io.File) = IO(file.delete()).void
  }

  object FailedGlobalStub extends GlobalResource {
    def sharedResources(store: GlobalWrite): Resource[IO, Unit] =
      Resource.eval(IO.raiseError(new Exception("Global Boom")))
  }

  class TmpFileSuite(global: GlobalRead) extends IOSuite {
    type Res = File
    def sharedResource: Resource[IO, File] =
      global.getOrFailR[File]()

    test("electric boo") { (file, log) =>
      for {
        _          <- log.info(s"file:${file.getAbsolutePath()}")
        fileExists <- IO(file.exists())
      } yield expect(fileExists) and failure("forcing logs dispatch")
    }
  }

  object SetTimeUnsafeRun extends CatsUnsafeRun {
    override def realTimeMillis: IO[Long] = IO.pure(0L)
  }

  class LazyState(
      initialised: IO[Int],
      finalised: IO[Int],
      totalUses: CECompat.Ref[IO, Int],
      uses: CECompat.Ref[IO, Int]) {
    val getState: IO[(Int, Int, Int, Int)] = for {
      i <- initialised
      f <- finalised
      t <- totalUses.updateAndGet(_ + 1)
      u <- uses.updateAndGet(_ + 1)
    } yield (i, f, t, u)
  }

  object LazyGlobal extends GlobalResource {
    def sharedResources(global: weaver.GlobalWrite): Resource[IO, Unit] =
      CECompat.resourceLift {
        for {
          initialised <- CECompat.Ref[IO].of(0)
          finalised   <- CECompat.Ref[IO].of(0)
          totalUses   <- CECompat.Ref[IO].of(0)
          resource =
            CECompat.resourceLift(CECompat.Ref[IO].of(0)).flatMap { uses =>
              Resource.make(initialised.update(_ + 1))(_ =>
                finalised.update(_ + 1)).map(_ =>
                new LazyState(initialised.get, finalised.get, totalUses, uses))
            }
          _ <- global.putLazy(resource)
        } yield ()
      }
  }

  class LazyAccessParallel(global: GlobalRead) extends IOSuite {
    type Res = LazyState
    def sharedResource: Resource[IO, Res] = global.getOrFailR[LazyState]()

    test("Lazy resources should be instantiated only once") { state =>
      IO.sleep(100.millis) *> state.getState.map {
        case (initialised, finalised, totalUses, localUses) =>
          expect.all(
            initialised == 1, // resource is initialised only once and uses in parallel
            finalised == 0, // resource is not finalised until all parallel uses are completed
            totalUses >= 1,
            totalUses <= 3,
            localUses >= 1,
            localUses <= 3
          )
      }
    }
  }

  abstract class LazyAccessSequential(global: GlobalRead, index: Int)
      extends IOSuite {
    type Res = LazyState
    def sharedResource: Resource[IO, Res] =
      CECompat.resourceLift(IO.sleep(index * 500.millis)).flatMap(_ =>
        global.getOrFailR[LazyState]())

    test("Lazy resources should be instantiated several times") { state =>
      state.getState.map {
        case (initialised, finalised, totalUses, localUses) =>
          expect.all(
            initialised == totalUses, // lazy resource will get initialised for each suite
            finalised == totalUses - 1,
            localUses == 1 // one test for each inialisation
          )
      }
    }
  }

  // Using sleeps to force sequential runs of suites
  class LazyAccessSequential0(global: GlobalRead)
      extends LazyAccessSequential(global, 0)
  class LazyAccessSequential1(global: GlobalRead)
      extends LazyAccessSequential(global, 1)
  class LazyAccessSequential2(global: GlobalRead)
      extends LazyAccessSequential(global, 2)

}
