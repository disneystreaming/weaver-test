package org.jetbrains.plugins.scala.testingSupport.test

import com.intellij.execution.RunConfigurationExtension
import com.intellij.execution.configurations.{
  JavaParameters,
  RunConfigurationBase,
  RunnerSettings
}
import com.intellij.util.PathUtil
import org.jetbrains.plugins.scala.project._

class WeaverTestRunConfigurationExtension extends RunConfigurationExtension {
  override def updateJavaParameters[T <: RunConfigurationBase[_]](
      t: T,
      javaParameters: JavaParameters,
      runnerSettings: RunnerSettings
  ): Unit = {

    val module = t.asInstanceOf[WeaverTestRunConfiguration].getModule

    val runnerJarName = module.scalaSdk match {
      case Some(lib) => lib.compilerVersion match {
          case Some(version) if version.startsWith("2.12") => "/ideaRunner_2_12.jar"
          case Some(version) if version.startsWith("2.13") => "/ideaRunner_2_13.jar"
          case unsupported => throw new RuntimeException(
              "Can't run test in Module. Unsupported compiler version" + unsupported)
        }
      case None => throw new RuntimeException(
          "Can't run test in Module. scala SDK is not specified to module" + module.getName)
    }

    val runnerJar = PathUtil
      .getJarPathForClass(classOf[WeaverTestRunConfigurationExtension])
      .replace("/ideaPlugin.jar", runnerJarName)
    javaParameters.getClassPath.add(runnerJar)
  }
  override def isApplicableFor(
      runConfigurationBase: RunConfigurationBase[_]
  ): Boolean = runConfigurationBase.isInstanceOf[WeaverTestRunConfiguration]
}
