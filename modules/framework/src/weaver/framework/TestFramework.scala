package weaver.framework

import sbt.testing.{ Framework => BaseFramework, Runner => BaseRunner, _ }

@deprecated("Weaver is now effect-specific", "0.6.0")
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
    |Effect-type specific configuration is now required in your build.
    |
    |## cats-effect
    |
    |libraryDependencies += "com.disneystreaming" %% "weaver-cats" % "x.y.z"
    |testFrameworks += new TestFramework("weaver.framework.CatsEffect")
    |
    |## monix
    |
    |libraryDependencies += "com.disneystreaming" %% "weaver-monix" % "x.y.z"
    |testFrameworks += new TestFramework("weaver.framework.Monix")
    |
    |## monix-bio
    |
    |libraryDependencies += "com.disneystreaming" %% "weaver-monix-bio" % "x.y.z"
    |testFrameworks += new TestFramework("weaver.framework.MonixBIO")
    |
    |## zio
    |
    |libraryDependencies += "com.disneystreaming" %% "weaver-zio" % "x.y.z"
    |testFrameworks += new TestFramework("weaver.framework.ZIO")
    |
    |---
    |
    |For more details, please refer yourself to the documentation:
    |https://disneystreaming.github.io/weaver-test/docs/installation
    |
    |We apologise for the inconvenience.
    |
    |Kind regards, from the weaver-test maintainers
    |
    |###############################################################################
  """.stripMargin

  private def crash(): Nothing =
    throw new Exception(message) with scala.util.control.NoStackTrace

}

object TestFramework {}
