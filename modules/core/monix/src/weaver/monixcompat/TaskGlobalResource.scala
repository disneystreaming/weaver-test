package weaver
package monixcompat

import cats.effect.Resource

import monix.eval.Task

trait TaskGlobalResource extends GlobalResourceF[Task] {
  def sharedResources(global: GlobalWrite): Resource[Task, Unit]
}
