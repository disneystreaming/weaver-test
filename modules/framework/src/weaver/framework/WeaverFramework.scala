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

  // val weaverFingerprints: WeaverFingerprints[IO] = CatsFingerprints

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

  def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader): BaseRunner = {
    new WeaverRunner[F](
      args,
      remoteArgs,
      fp.suiteLoader(testClassLoader),
      unsafeRun)
  }

  def slaveRunner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader,
      send: String => Unit): BaseRunner = {
    discard[String => Unit](send)
    runner(args, remoteArgs, testClassLoader)
  }
}
