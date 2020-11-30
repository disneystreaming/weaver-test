package weaver
package framework

import weaver.{ Platform, discard }

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }

class WeaverFramework[F[_]](
    suffix: String,
    val fp: WeaverFingerprints[F],
    val unsafeRun: UnsafeRun[F])
    extends BaseFramework {

  def name(): String = s"weaver-$suffix"

  def fingerprints(): Array[Fingerprint] =
    if (Platform.isJVM) {
      Array(
        fp.GlobalResourcesFingerprint,
        fp.SuiteFingerprint,
        fp.ResourceSharingSuiteFingerprint
      )
    } else {
      Array(fp.SuiteFingerprint)
    }

  def weaverRunner(
    args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader,
      send: Option[String => Unit]
  ): WeaverRunner[F] = {
    new WeaverRunner[F](
      args,
      remoteArgs,
      fp.suiteLoader(testClassLoader),
      unsafeRun,
      send)
  }

  def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader): BaseRunner = {
    weaverRunner(args, remoteArgs, testClassLoader, None)
  }

  def slaveRunner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader,
      send: String => Unit): BaseRunner = {
    discard[String => Unit](send)
    weaverRunner(args, remoteArgs, testClassLoader, Some(send))
  }
}
