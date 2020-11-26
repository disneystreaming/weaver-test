package weaver
package framework

import weaver.{ Platform, discard }

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }

class AbstractFramework[F[_]](
    suffix: String,
    weaverFingerprints: WeaverFingerprints[F],
    unsafeRun: UnsafeRun[F])
    extends BaseFramework {

  def name(): String = s"weaver-$suffix"

  // val weaverFingerprints: WeaverFingerprints[IO] = CatsFingerprints

  def fingerprints(): Array[Fingerprint] =
    if (Platform.isJVM) {
      Array(
        weaverFingerprints.GlobalResourcesFingerprint,
        weaverFingerprints.SuiteFingerprint,
        weaverFingerprints.ResourceSharingSuiteFingerpring
      )
    } else {
      Array(weaverFingerprints.SuiteFingerprint)
    }

  def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader): BaseRunner = {
    new WeaverRunner[F](
      args,
      remoteArgs,
      weaverFingerprints.suiteLoader(testClassLoader),
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
