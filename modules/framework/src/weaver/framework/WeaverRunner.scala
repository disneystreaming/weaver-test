package weaver
package framework

import java.io.PrintStream

import sbt.testing._

class WeaverRunner[F[_]](
    val args: Array[String],
    val rmArgs: Array[String],
    val suiteLoader: SuiteLoader[F],
    val unsafeRun: UnsafeRun[F],
    val channel: Option[String => Unit],
    val errorStream: PrintStream
) extends Runner
    with RunnerCompat[F] {
  override def remoteArgs(): Array[String] = rmArgs
}

final case class SuiteName(name: String) extends AnyVal
