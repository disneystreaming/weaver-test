package weaver

import scala.concurrent.ExecutionContext

import org.scalajs.macrotaskexecutor.MacrotaskExecutor

object PlatformECCompat {
  val ec: ExecutionContext = MacrotaskExecutor
}
