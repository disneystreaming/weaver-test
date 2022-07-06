package weaver

import scala.concurrent.ExecutionContext

import org.scalajs.macrotaskexecutor.MacrotaskExecutor

private[weaver] object PlatformECCompat {
  val ec: ExecutionContext = MacrotaskExecutor
}
