package weaver
package framework

import sbt.testing._

class WeaverRunner[F[_]](
    val args: Array[String],
    val remoteArgs: Array[String],
    val suiteLoader: SuiteLoader[F],
    val unsafeRun: UnsafeRun[F],
    val channel: Option[String => Unit]
) extends Runner with RunnerCompat[F]

final case class SuiteName(name: String) extends AnyVal
