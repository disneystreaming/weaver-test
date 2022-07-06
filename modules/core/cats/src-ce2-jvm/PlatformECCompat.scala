package weaver

import scala.concurrent.ExecutionContext

object PlatformECCompat {
  val ec: ExecutionContext = ExecutionContext.global
}
