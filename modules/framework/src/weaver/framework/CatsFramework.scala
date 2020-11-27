package weaver
package framework

import cats.effect.IO

class CatsFramework
    extends WeaverFramework("cats-effect", CatsFingerprints, CatsUnsafeRun)

object CatsFingerprints
    extends WeaverFingerprints.Mixin[IO, BaseIOSuite, IOGlobalResourcesInit]

trait IOGlobalResourcesInit extends GlobalResourcesInit[IO]

