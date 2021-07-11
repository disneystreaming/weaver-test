package weaver
package monixbiocompat

import monix.execution.Scheduler

object PlatformCompat {
  def runSync[A](task: monix.bio.Task[A])(implicit scheduler: Scheduler) = {
    val _ = scheduler
    throw new Exception("Cannot block on JS!")
  }

  def defaultScheduler: Scheduler = Scheduler.global

}
