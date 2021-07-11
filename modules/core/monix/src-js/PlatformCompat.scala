package weaver
package monixcompat

import monix.execution.Scheduler

object PlatformCompat {
  def runSync[A](task: monix.eval.Task[A])(implicit scheduler: Scheduler) = {
    val _ = scheduler
    throw new Exception("Cannot block on JS!")
  }

  def defaultScheduler: Scheduler = Scheduler.global
}
