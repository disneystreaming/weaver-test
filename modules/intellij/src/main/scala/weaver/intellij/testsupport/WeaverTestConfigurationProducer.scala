package weaver.intellij.testsupport

import com.intellij.execution.Location
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.{ PsiDocumentManager, PsiElement }
import org.jetbrains.plugins.scala.extensions.PsiElementExt
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer.CreateFromContextInfo
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestConfigurationProducer.CreateFromContextInfo.ClassWithTestName

class WeaverTestConfigurationProducer
    extends AbstractTestConfigurationProducer[WeaverTestRunConfiguration] {

  override def getConfigurationFactory: ConfigurationFactory =
    WeaverTestConfigurationType.configurationFactory

  override protected def suitePaths: Seq[String] =
    WeaverTestFramework.baseSuitePaths

  override protected def configurationName(
      contextInfo: AbstractTestConfigurationProducer.CreateFromContextInfo): String =
    contextInfo match {
      case CreateFromContextInfo.AllInPackage(_, packageName) =>
        s"Weaver-test in ''$packageName''"
      case ClassWithTestName(testClass, _) => testClass.name
    }

  private def getTestName(element: PsiElement) = {
    val file = element.getContainingFile
    val document =
      PsiDocumentManager.getInstance(element.getProject).getDocument(file)
    val line: Int    = document.getLineNumber(element.getTextRange.getStartOffset)
    val lineStartAt1 = line + 1

    getTestClass(element).map(clazz =>
      ClassWithTestName(clazz.testClass, Some(s"line://$lineStartAt1")))
  }

  private def getTestClass(element: PsiElement): Option[ClassWithTestName] = {
    val testClassDef: ScTypeDefinition =
      PsiTreeUtil.getParentOfType(element, classOf[ScTypeDefinition], false)
    Option(testClassDef).flatMap { testClassDef =>
      val suiteClasses = suitePaths.flatMap {
        element.elementScope.getCachedClass(_)
      }
      if (suiteClasses.exists(ScalaPsiUtil.isInheritorDeep(testClassDef, _))) {
        Some(ClassWithTestName(testClassDef, None))
      } else None
    }
  }

  override def getTestClassWithTestName(
      location: Location[_ <: PsiElement]
  ): Option[ClassWithTestName] = {
    val element = location.getPsiElement
    element match {
      case WeaverTestMethod() => getTestName(element)
      case _                  => getTestClass(element)
    }
  }
}

object WeaverTestConfigurationProducer extends WeaverTestConfigurationProducer
