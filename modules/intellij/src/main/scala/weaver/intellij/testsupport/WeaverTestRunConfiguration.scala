package weaver.intellij.testsupport

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.testIntegration.TestFramework
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration.TestFrameworkRunnerInfo
import org.jetbrains.plugins.scala.testingSupport.test.sbt.{
  SbtCommandsBuilder,
  SbtCommandsBuilderBase,
  SbtTestRunningSupport,
  SbtTestRunningSupportBase
}
import org.jetbrains.plugins.scala.testingSupport.test.{
  AbstractTestConfigurationProducer,
  AbstractTestRunConfiguration,
  SuiteValidityChecker
}

class WeaverTestRunConfiguration(
    project: Project,
    override val configurationFactory: ConfigurationFactory
) extends AbstractTestRunConfiguration(
      project,
      configurationFactory,
      "Weaver"
    ) {

  override def configurationProducer: AbstractTestConfigurationProducer[_] =
    WeaverTestConfigurationProducer

  override def testFramework: TestFramework = WeaverTestFramework

  override protected def validityChecker: SuiteValidityChecker =
    (clazz: PsiClass, suiteClass: PsiClass) => {
      clazz.isInstanceOf[ScObject] &&
        ScalaPsiUtil.isInheritorDeep(clazz, suiteClass)
    }

  override protected def runnerInfo: AbstractTestRunConfiguration.TestFrameworkRunnerInfo =
    TestFrameworkRunnerInfo(
      "weaver.intellij.runner.WeaverTestRunner")

  override val sbtSupport: SbtTestRunningSupport =
    new SbtTestRunningSupportBase {
      override def commandsBuilder: SbtCommandsBuilder =
        new SbtCommandsBuilderBase {}

      override def allowsSbtUiRun: Boolean = true
    }

  override def suitePaths: Seq[String] = WeaverTestFramework.baseSuitePaths

}
