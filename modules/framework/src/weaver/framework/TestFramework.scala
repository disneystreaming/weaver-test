package weaver.framework

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }

@deprecated
class TestFramework extends BaseFramework {

  def name(): String = crash()

  def fingerprints(): Array[Fingerprint] = {
    crash()
  }

  def runner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader): BaseRunner =
    crash()

  def slaveRunner(
      args: Array[String],
      remoteArgs: Array[String],
      testClassLoader: ClassLoader,
      send: String => Unit): BaseRunner = {
    crash()
  }

  val message: String = """
    |###############################################################################
    |
    |If you're reading this, you have recently upgraded weaver to a version > 0.5.x .
    |Effect-type specific configuration is now required in your build. Please refer yourself
    |to the release page https://github.com/disneystreaming/weaver-test/releases
    |
    |We apologise for the inconvenience.
    |
    |Kind regards, from the weaver-test maintainers
    |
    |###############################################################################
  """.stripMargin

  private def crash(): Nothing = throw new Exception(message)

}

object TestFramework {}
