package weaver
package framework

import weaver.ziocompat.{ BaseZIOSuite, T, ZIOGlobalResource, ZIOUnsafeRun }

import ZIOUnsafeRun.effect

class ZIO extends WeaverFramework("zio", ZIOFingerprints, ZIOUnsafeRun)

object ZIOFingerprints
    extends WeaverFingerprints.Mixin[T, BaseZIOSuite, ZIOGlobalResource]
