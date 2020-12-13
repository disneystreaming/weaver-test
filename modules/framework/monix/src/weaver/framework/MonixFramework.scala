package weaver
package framework

import weaver.monixcompat.{ BaseTaskSuite, MonixUnsafeRun, TaskGlobalResource }

import monix.eval.Task

class Monix extends WeaverFramework("monix", MonixFingerprints, MonixUnsafeRun)

object MonixFingerprints
    extends WeaverFingerprints.Mixin[Task, BaseTaskSuite, TaskGlobalResource]
