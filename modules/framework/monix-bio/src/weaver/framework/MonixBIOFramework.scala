package weaver
package framework

import weaver.monixbiocompat.{BaseIOSuite, MonixBioUnsafeRun}

import monix.bio.Task

class MonixBIO
    extends WeaverFramework("monix-bio", MonixFingerprints, MonixBioUnsafeRun)

object MonixFingerprints
    extends WeaverFingerprints.Mixin[Task, BaseIOSuite, MonixGlobalResource]

trait MonixGlobalResource extends GlobalResource[Task]
