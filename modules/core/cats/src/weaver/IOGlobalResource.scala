package weaver

import cats.effect.{ IO, Resource }

trait IOGlobalResource extends GlobalResourceF[IO] {
  def sharedResources(global: GlobalWrite): Resource[IO, Unit]
}
