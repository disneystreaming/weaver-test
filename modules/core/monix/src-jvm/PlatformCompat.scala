package weaver
package monixcompat

import monix.execution.Scheduler

object PlatformCompat {
  def runSync[A](task: monix.eval.Task[A])(implicit scheduler: Scheduler) =
    task.runSyncUnsafe()

  def defaultScheduler: Scheduler = Scheduler.fixedPool(
    "weaver-monix",
    java.lang.Runtime.getRuntime().availableProcessors() - 1)

}
