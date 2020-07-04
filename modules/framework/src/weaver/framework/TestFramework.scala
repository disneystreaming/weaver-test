package weaver.framework

import weaver.{ Platform, discard }

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }

class TestFramework extends BaseFramework {

  def name(): String = "weaver"

  def fingerprints(): Array[Fingerprint] =
    if (Platform.isJVM) {
      Array(TestFramework.GlobalResourcesFingerprint,
            TestFramework.ModuleFingerprint,
            TestFramework.GlobalResourcesSharingFingerprint)
    } else {
      Array(TestFramework.ModuleFingerprint)
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
  object ModuleFingerprint extends WeaverFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String           = "weaver.BaseSuiteClass"
  }

  /**
   * A fingerprint that searches only for classes extending [[weaver.EffectSuite]].
   * that have a constructor that takes a single [[weaver.GlobalResources]] parameter.
   */
  object GlobalResourcesSharingFingerprint extends WeaverFingerprint {
    val isModule                           = false
    def requireNoArgConstructor(): Boolean = false
    def superclassName(): String           = "weaver.BaseSuiteClass"
  }

  /**
   * A fingerprint that searches only for singleton objects
   * of type [[weaver.GlobalResourcesInit]].
   */
  object GlobalResourcesFingerprint extends WeaverFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String           = "weaver.GlobalResourcesInit"
  }

  trait WeaverFingerprint extends SubclassFingerprint {
    def unapply(taskDef: TaskDef): Option[TaskDef] = taskDef.fingerprint match {
      case sf: SubclassFingerprint if fingerprintMatches(sf) => Some(taskDef)
      case _                                                 => None
    }

    private def fingerprintMatches(sf: SubclassFingerprint): Boolean = {
      sf.isModule() == this.isModule() &&
      sf.requireNoArgConstructor() == this.requireNoArgConstructor() &&
      sf.superclassName() == this.superclassName()
    }
  }
}
