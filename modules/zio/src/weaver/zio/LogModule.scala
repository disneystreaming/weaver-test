package weaver.zio

import weaver.Log
import _root_.zio.UIO

/**
 *  Accommodating ZIO's reader parameter
 */
trait LogModule {

  def log: Log[UIO]

}
