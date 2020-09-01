package weaver.intellij.testsupport

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.{
  JavaParameters,
  RunConfigurationBase,
  RunnerSettings
}
import org.jetbrains.plugins.scala.project._

class WeaverTestRunConfigurationExtension extends RunConfigurationExtension {

  override def updateJavaParameters[T <: RunConfigurationBase[_]](
      t: T,
      javaParameters: JavaParameters,
      runnerSettings: RunnerSettings
  ): Unit = {

    val module = t.asInstanceOf[WeaverTestRunConfiguration].getModule
    // format == sbt: $org:$artifact:$version:jar
    val (scalaVersionSuffix, weaverVersion) =
      module.libraries.map(_.getName).find {
        _.startsWith("sbt: com.disneystreaming:weaver-core")
      }.map { lib =>
        val parts              = lib.split(":")
        val weaverVersion      = parts(3)
        val scalaVersionSuffix = parts(2).split("_")(1)
        scalaVersionSuffix -> weaverVersion
      }.getOrElse(throw new RuntimeException("Weaver version not found"))

    import coursier._
    val runnerDep = coursier.parse.DependencyParser.dependency(
      s"com.disneystreaming:weaver-intellij-runner_$scalaVersionSuffix:$weaverVersion",
      "2.13.2") match {
      case Right(value) => value
      case Left(error)  => throw new RuntimeException(error)
    }
    println(runnerDep)
    val files = Fetch().addDependencies(runnerDep).run()
    println(files)

    files.foreach(javaParameters.getClassPath().add)
  }
  override def isApplicableFor(
      runConfigurationBase: RunConfigurationBase[_]
  ): Boolean = runConfigurationBase.isInstanceOf[WeaverTestRunConfiguration]
}
