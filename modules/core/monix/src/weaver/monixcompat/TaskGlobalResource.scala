package weaver
package monixcompat

import monix.eval.Task
import cats.effect.Resource

trait TaskGlobalResource extends GlobalResourceF[Task] {
  def sharedResources(global: GlobalWrite): Resource[Task, Unit]
}
