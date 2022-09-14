package weaver
package framework
package test

import java.io.File

import cats.effect._
import cats.effect.std.{ CyclicBarrier, Semaphore }
import cats.syntax.all._

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
      totalUses: Ref[IO, Int],
      uses: Ref[IO, Int],
      latch: IO[Unit]
  ) {
    def getState(parallelWait: Boolean): IO[(Int, Int, Int, Int)] = for {
      _ <- latch.whenA(parallelWait)
      i <- initialised
      f <- finalised
      t <- totalUses.updateAndGet(_ + 1)
      u <- uses.updateAndGet(_ + 1)
    } yield (i, f, t, u)
  }

  case class SequentialAccess(permit: Resource[IO, Unit])

  object LazyGlobal extends GlobalResource {
    def sharedResources(global: weaver.GlobalWrite): Resource[IO, Unit] =
      Resource.eval {
        for {
          initialised <- Ref[IO].of(0)
          finalised   <- Ref[IO].of(0)
          totalUses   <- Ref[IO].of(0)
          /**
           * NOTE: the number 3 refers to the current number of instantiated
           * suites in the DogFoodTestsJVM spec, tests involving "global lazy
           * resources"
           *
           * If you do either of the following:
           *
           *   - Change the number of LazyAccessParallel suites in "global lazy
           *     resources (parallel)" test
           *
           *   - Change the number below 3. Add another test to the
           *     LazyAccessParallel spec below
           *
           * You are very likely to face a very confusing non-deterministic
           * behaviour. Those numbers need to be kept in sync.
           */
          parallelLatch <- CyclicBarrier[IO](3)
          resource =
            Resource.eval(Ref[IO].of(0)).flatMap { uses =>
              val resourceInitialisation =
                Resource.make(initialised.update(_ + 1))(_ =>
                  finalised.update(_ + 1))

              val synchronisation =
                parallelLatch.await

              resourceInitialisation.as(new LazyState(initialised.get,
                                                      finalised.get,
                                                      totalUses,
                                                      uses,
                                                      synchronisation))
            }
          sequential <-
            Semaphore[IO](1).map(_.permit).map(SequentialAccess.apply)
          _ <- global.putLazy(resource)
          _ <- global.put(sequential)
        } yield ()
      }
  }

  class LazyAccessParallel(global: GlobalRead) extends IOSuite {
    type Res = LazyState
    def sharedResource: Resource[IO, Res] = global.getOrFailR[LazyState]()

    test("Lazy resources should be instantiated only once") { state =>
      state.getState(parallelWait = true).map {
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
    def sharedResource: Resource[IO, Res] = {
      global.getOrFailR[SequentialAccess]().flatMap { semRes =>
        semRes.permit.flatMap { _ =>
          global.getOrFailR[LazyState]()
        }
      }
    }

    test("Lazy resources should be instantiated several times") { state =>
      state.getState(parallelWait = false).map {
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
