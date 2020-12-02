package weaver.framework

import scala.concurrent.ExecutionContext

object TrampolineEC {

  /**
   * [[TrampolineEC]] instance that executes everything
   * immediately, on the current thread.
   *
   * Implementation notes:
   *
   *  - if too many `blocking` operations are chained, at some point
   *    the implementation will trigger a stack overflow error
   *  - `reportError` re-throws the exception in the hope that it
   *    will get caught and reported by the underlying thread-pool,
   *    because there's nowhere it could report that error safely
   *    (i.e. `System.err` might be routed to `/dev/null` and we'd
   *    have no way to override it)
   *
   * INTERNAL API.
   */
  val immediate: ExecutionContext =
    new ExecutionContext {
      def execute(r: Runnable): Unit = r.run()
      def reportFailure(e: Throwable): Unit =
        e.printStackTrace()
    }

}
