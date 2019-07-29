package weaver.ziocompat

import weaver.Log
import zio.UIO

/**
 *  Accommodating ZIO's reader parameter
 */
trait LogModule {

  def log: Log[UIO]

}
