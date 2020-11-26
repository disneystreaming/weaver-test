package weaver
package framework

import cats.effect.IO
import cats.effect.ContextShift

import scala.concurrent.ExecutionContext

class CatsFramework
    extends AbstractFramework("cats-effect", CatsFingerprints, CatsUnsafeRun)

object CatsFingerprints
    extends WeaverFingerprints.Mixin[IO, BaseIOSuite, IOGlobalResourcesInit]

trait IOGlobalResourcesInit extends GlobalResourcesInit[IO]

object CatsUnsafeRun extends UnsafeRun[IO] {

  implicit val contextShift: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)
  implicit val concurrent = IO.ioConcurrentEffect
  implicit val parallel   = IO.ioParallel

  def void: IO[Unit] = IO.unit

  def background(task: IO[Unit]): IO[Unit] =
    task.unsafeRunCancelable {
      case Left(error) => error.printStackTrace
      case Right(_)    => ()
    }

  def sync(task: IO[Unit]): Unit = task.unsafeRunSync()

}
