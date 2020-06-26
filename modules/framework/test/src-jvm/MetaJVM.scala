package weaver
package framework
package test

import cats.effect._

import java.io.File

// The build tool will only detect and run top-level test suites. We can however nest objects
// that contain failing tests, to allow for testing the framework without failing the build
// because the framework will have ran the tests on its own.
object MetaJVM {
  object MutableSuiteTest extends MutableSuiteTest

  object GlobalStub extends GlobalResourcesInit {
    def sharedResources(store: GlobalResources.Write[IO]): Resource[IO, Unit] =
      Resource.make(makeTmpFile)(deleteFile).flatMap { file =>
        store.putR(file)
      }

    val makeTmpFile                    = IO(java.io.File.createTempFile("hello", ".tmp"))
    def deleteFile(file: java.io.File) = IO(file.delete()).void
  }

  class TmpFileSuite(global: GlobalResources) extends IOSuite {
    type Res = File
    def sharedResource: Resource[IO, File] =
      global.in[IO].getOrFailR[File]()

    test("electric boo") { (file, log) =>
      for {
        _          <- log.info(s"file:${file.getAbsolutePath()}")
        fileExists <- IO(file.exists())
      } yield expect(fileExists) and failure("forcing logs dispatch")
    }
  }

}
