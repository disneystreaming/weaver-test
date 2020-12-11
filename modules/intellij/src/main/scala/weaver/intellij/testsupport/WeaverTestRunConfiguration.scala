package weaver.intellij.testsupport

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScObject
import org.jetbrains.plugins.scala.testingSupport.test.{
  AbstractTestConfigurationProducer,
  AbstractTestRunConfiguration,
  SuiteValidityChecker
}
import org.jetbrains.plugins.scala.testingSupport.test.RunStateProvider
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestFramework
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import org.jetbrains.plugins.scala.testingSupport.test.ScalaTestFrameworkCommandLineState
import org.jetbrains.plugins.scala.testingSupport.test.CustomTestRunnerBasedStateProvider.TestFrameworkRunnerInfo

class WeaverTestRunConfiguration(
    project: Project,
    override val configurationFactory: ConfigurationFactory
) extends AbstractTestRunConfiguration(
      project,
      configurationFactory,
      "Weaver"
    ) { self =>

  override def configurationProducer: AbstractTestConfigurationProducer[_] =
    WeaverTestConfigurationProducer

  override def testFramework: AbstractTestFramework = WeaverTestFramework

  override protected def validityChecker: SuiteValidityChecker =
    (clazz: PsiClass, suiteClass: PsiClass) => {
      clazz.isInstanceOf[ScObject] &&
        ScalaPsiUtil.isInheritorDeep(clazz, suiteClass)
    }

  def runStateProvider: RunStateProvider = new RunStateProvider {
    def commandLineState(
        env: ExecutionEnvironment,
        failedTests: Option[Seq[(String, String)]]): RunProfileState =
      new WeaverTestCommandLineState(env, failedTests)
  }

  private def runnerInfo =
    TestFrameworkRunnerInfo("weaver.intellij.runner.WeaverTestRunner")

  class WeaverTestCommandLineState(
      env: ExecutionEnvironment,
      failedTests: Option[Seq[(String, String)]])
      extends ScalaTestFrameworkCommandLineState(self,
                                                 env,
                                                 failedTests,
                                                 runnerInfo) {}

}
