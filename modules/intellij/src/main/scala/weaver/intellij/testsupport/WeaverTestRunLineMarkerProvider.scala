package weaver.intellij.testsupport

import com.intellij.execution.lineMarker.{
  ExecutorAction,
  RunLineMarkerContributor
}
import com.intellij.icons.AllIcons.RunConfigurations.TestState
import com.intellij.psi.PsiElement
import com.intellij.testIntegration.TestRunLineMarkerProvider

class WeaverTestRunLineMarkerProvider extends TestRunLineMarkerProvider {

  override def getInfo(e: PsiElement): RunLineMarkerContributor.Info = {
    if (WeaverTestFramework.isTestMethod(e)) {
      new RunLineMarkerContributor.Info(
        TestState.Green2,
        //TODO Use ScalaBundle.message
        (_: PsiElement) => "Run Test",
        ExecutorAction.getActions(1): _*
      )
    } else null
  }
}
