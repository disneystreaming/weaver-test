package weaver
package framework

import java.io.PrintStream

import weaver.ziocompat.{ BaseZIOSuite, T, ZIOGlobalResource, ZIOUnsafeRun }

import ZIOUnsafeRun.effect

class ZIO(errorStream: PrintStream)
    extends WeaverFramework("zio", ZIOFingerprints, ZIOUnsafeRun, errorStream) {
  def this() = this(System.err)
}

object ZIOFingerprints
    extends WeaverFingerprints.Mixin[T, BaseZIOSuite, ZIOGlobalResource]
