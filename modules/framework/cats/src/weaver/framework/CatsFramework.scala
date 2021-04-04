package weaver
package framework

import java.io.PrintStream

import cats.effect.IO

class CatsEffect(errorStream: PrintStream)
    extends WeaverFramework("cats-effect",
                            CatsFingerprints,
                            CatsUnsafeRun,
                            errorStream) {
  def this() = this(System.err)
}

object CatsFingerprints
    extends WeaverFingerprints.Mixin[IO, BaseCatsSuite, IOGlobalResource]
