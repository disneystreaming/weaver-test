package weaver.intellij.testsupport

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{
  ScObject,
  ScTemplateDefinition
}
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestFramework.TestFrameworkSetupInfo
import org.jetbrains.plugins.scala.testingSupport.test.{
  AbstractTestFramework,
  TestFrameworkSetupSupportBase
}

class WeaverTestFramework
    extends AbstractTestFramework
    with TestFrameworkSetupSupportBase {

  override def testFileTemplateName: String = "Weaver Test"

  override val getMarkerClassFQName: String = "weaver.Suite"

  override val getName: String = "Weaver"

  override def getDefaultSuperClass: String = "weaver.SimpleIOSuite"

  def baseSuitePaths: Seq[String] = List("weaver.Suite")

  def frameworkSetupInfo(scalaVersion: Option[String]) =
    TestFrameworkSetupInfo(
      Seq(""""com.disneystreaming" %% "weaver-core" % "latest.integration" % "test""""),
      Seq())

  override def isTestMethod(element: PsiElement): Boolean = element match {
    case WeaverTestMethod() => true
    case _                  => false
  }

  override protected def isTestClass(
      definition: ScTemplateDefinition): Boolean =
    definition.isInstanceOf[ScObject] && super.isTestClass(definition)
}

object WeaverTestFramework extends WeaverTestFramework
