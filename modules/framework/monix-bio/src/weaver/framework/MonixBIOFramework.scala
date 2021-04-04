package weaver
package framework

import java.io.PrintStream

import weaver.monixbiocompat.{
  BaseIOSuite,
  IOGlobalResource,
  MonixBIOUnsafeRun
}

import monix.bio.Task

class MonixBIO(errorStream: PrintStream)
    extends WeaverFramework("monix-bio",
                            MonixBIOFingerprints,
                            MonixBIOUnsafeRun,
                            errorStream) {
  def this() = this(System.err)
}

object MonixBIOFingerprints
    extends WeaverFingerprints.Mixin[Task, BaseIOSuite, IOGlobalResource]
