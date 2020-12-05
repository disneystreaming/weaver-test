package weaver
package monixcompat

import monix.execution.Scheduler

object PlatformCompat {
  def runSync(task: monix.eval.Task[Unit])(implicit scheduler: Scheduler) = {
    val _ = scheduler
  }
}
