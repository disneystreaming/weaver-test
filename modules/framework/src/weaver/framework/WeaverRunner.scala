package weaver
package framework

import sbt.testing._

class WeaverRunner[F[_]](
    val args: Array[String],
    val remoteArgs: Array[String],
    val suiteLoader: SuiteLoader[F],
    val unsafeRun: UnsafeRun[F]
) extends Runner with PlatformRunner[F]

final case class SuiteName(name: String) extends AnyVal
