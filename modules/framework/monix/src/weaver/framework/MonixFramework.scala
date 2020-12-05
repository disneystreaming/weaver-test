package weaver
package framework

import weaver.monixcompat.{BaseTaskSuite, MonixUnsafeRun}

import monix.eval.Task

class Monix extends WeaverFramework("monix", MonixFingerprints, MonixUnsafeRun)

object MonixFingerprints
    extends WeaverFingerprints.Mixin[Task, BaseTaskSuite, MonixGlobalResource]

trait MonixGlobalResource extends GlobalResource[Task]
