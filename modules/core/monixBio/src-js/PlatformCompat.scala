package weaver
package monixbiocompat

import monix.execution.Scheduler

object PlatformCompat {
  def runSync(task: monix.bio.Task[Unit])(implicit scheduler: Scheduler) = {
    val _ = scheduler
  }
}
