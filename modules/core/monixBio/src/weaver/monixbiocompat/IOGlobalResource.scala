package weaver
package monixbiocompat

import monix.bio.Task
import cats.effect.Resource

trait IOGlobalResource extends GlobalResourceF[Task] {
  def sharedResources(global: GlobalWrite): Resource[Task, Unit]
}
