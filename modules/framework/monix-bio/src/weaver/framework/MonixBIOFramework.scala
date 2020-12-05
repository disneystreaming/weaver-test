package weaver
package framework

import weaver.monixbiocompat.{BaseIOSuite, IOGlobalResource, MonixBIOUnsafeRun}

import monix.bio.Task

class MonixBIO
    extends WeaverFramework("monix-bio",
                            MonixBIOFingerprints,
                            MonixBIOUnsafeRun)

object MonixBIOFingerprints
    extends WeaverFingerprints.Mixin[Task, BaseIOSuite, IOGlobalResource]
