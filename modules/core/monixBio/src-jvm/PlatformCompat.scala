package weaver
package monixbiocompat

import monix.execution.Scheduler

object PlatformCompat {
  def runSync[A](task: monix.bio.Task[A])(implicit scheduler: Scheduler) =
    task.runSyncUnsafe()

  def defaultScheduler: Scheduler = Scheduler.fixedPool(
    "weaver-monix",
    java.lang.Runtime.getRuntime().availableProcessors() - 1)
}
