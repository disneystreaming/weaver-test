package weaver
package framework

import cats.effect.IO

class CatsEffect
    extends WeaverFramework("cats-effect", CatsFingerprints, CatsUnsafeRun)

object CatsFingerprints
    extends WeaverFingerprints.Mixin[IO, BaseCatsSuite, IOGlobalResource]
