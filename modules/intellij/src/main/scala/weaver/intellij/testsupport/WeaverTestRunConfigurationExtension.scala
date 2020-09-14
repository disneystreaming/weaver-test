package weaver.intellij.testsupport

import java.io.File

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.{
  JavaParameters,
  RunConfigurationBase,
  RunnerSettings
}
import org.jetbrains.plugins.scala.project._

class WeaverTestRunConfigurationExtension extends RunConfigurationExtension {

  override def updateJavaParameters[T <: RunConfigurationBase[_]](
      config: T,
      javaParameters: JavaParameters,
      runnerSettings: RunnerSettings
  ): Unit = {
    config match {
      case weaverConfig: WeaverTestRunConfiguration =>
        val files = fetchRunnerDeps(weaverConfig)
        files.foreach(javaParameters.getClassPath().add)
      case _ =>
    }
  }

  private def fetchRunnerDeps(
      weaverConfig: WeaverTestRunConfiguration): Seq[File] = {
    val module = weaverConfig.getModule
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
      case Right(value) => value.withTransitive(false)
      case Left(error)  => throw new RuntimeException(error)
    }
    Fetch().addDependencies(runnerDep).run()
  }

  override def isApplicableFor(
      runConfigurationBase: RunConfigurationBase[_]
  ): Boolean = runConfigurationBase.isInstanceOf[WeaverTestRunConfiguration]
}
