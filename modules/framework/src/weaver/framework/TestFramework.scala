package weaver.framework

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }
import weaver.discard

class TestFramework extends BaseFramework {

  def name(): String = "weaver"

  def fingerprints(): Array[Fingerprint] = {
    Array(TestFramework.GlobalResourcesFingerprint,
          TestFramework.ModuleFingerprint)
  }

  def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader): BaseRunner =
    new Runner(args, remoteArgs, testClassLoader)

  def slaveRunner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader,
      send: String => Unit): BaseRunner = {
    discard[String => Unit](send)
    runner(args, remoteArgs, testClassLoader)
  }
}

object TestFramework {

  /**
   * A fingerprint that searches only for singleton objects
   * of type [[weaver.EffectSuite]].
   */
  object ModuleFingerprint extends SubclassFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String           = "weaver.BaseSuiteClass"
  }

  /**
   * A fingerprint that searches only for classes extending [[weaver.EffectSuite]].
   * that have a constructor that takes a single [[weaver.GlobalResources]] parameter.
   */
  object GlobalResourcesSharingFingerprint extends SubclassFingerprint {
    val isModule                           = false
    def requireNoArgConstructor(): Boolean = false
    def superclassName(): String           = "weaver.BaseSuiteClass"
  }

  /**
   * A fingerprint that searches only for singleton objects
   * of type [[weaver.EffectSuite]].
   */
  object GlobalResourcesFingerprint extends SubclassFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String           = "weaver.GlobalResourcesInit"
  }
}
