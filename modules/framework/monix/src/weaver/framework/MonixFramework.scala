package weaver
package framework

import java.io.PrintStream

import weaver.monixcompat.{ BaseTaskSuite, MonixUnsafeRun, TaskGlobalResource }

import monix.eval.Task

class Monix(errorStream: PrintStream)
    extends WeaverFramework("monix",
                            MonixFingerprints,
                            MonixUnsafeRun,
                            errorStream) {
  def this() = this(System.err)
}

object MonixFingerprints
    extends WeaverFingerprints.Mixin[Task, BaseTaskSuite, TaskGlobalResource]
