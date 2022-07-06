package weaver

import scala.concurrent.ExecutionContext

private[weaver] object PlatformECCompat {
  val ec: ExecutionContext = ExecutionContext.global
}
