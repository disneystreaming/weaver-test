package weaver.framework

import weaver.{ Platform, discard }

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }

class Framework2 extends BaseFramework {

  def name(): String = "weaver"

  def fingerprints(): Array[Fingerprint] =
    if (Platform.isJVM) {
      Array(TestFramework.CatsIOFingerprint)
    } else {
      Array(TestFramework.CatsIOFingerprint)
    }

  def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader): BaseRunner =
    new CatsIORunner(args, remoteArgs, testClassLoader)

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
  object CatsIOFingerprint extends WeaverFingerprint {
    val isModule                           = true
    def requireNoArgConstructor(): Boolean = true
    def superclassName(): String           = "weaver.BaseIOSuite"
  }

  trait WeaverFingerprint extends SubclassFingerprint {
    def unapply(taskDef: TaskDef): Option[TaskDef] =
      taskDef.fingerprint() match {
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
