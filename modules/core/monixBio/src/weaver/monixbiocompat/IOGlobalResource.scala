package weaver
package monixbiocompat

import cats.effect.Resource

import monix.bio.Task

trait IOGlobalResource extends GlobalResourceF[Task] {
  def sharedResources(global: GlobalWrite): Resource[Task, Unit]
}
