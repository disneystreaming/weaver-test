package weaver.intellij.testsupport

import scala.collection.JavaConverters._

import com.intellij.codeInsight.daemon.impl.CollectHighlightsUtil
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.DependencyManagerBase.DependencyDescription
// import org.junit.runner.RunWith
// import org.junit.runners.AllTests

// @RunWith(classOf[AllTests])
class WeaverTestMethodTest extends WeaverFileSetTestCase("testMethod") {

  var myTestRootDisposable: TestDisposable = _

  override def setUp(): Unit = {
    super.setUp()
    myTestRootDisposable = new TestDisposable()
  }

  override def tearDown(): Unit = {
    Disposer.dispose(myTestRootDisposable)
    super.tearDown()
  }

  override protected def transform(data: Seq[String]): String = {
    addModuleDependencies()

    val psiFile            = createPseudoPhysicalScalaFile(data.head)
    val document: Document = getDocument(psiFile)

    val allPsiElements = CollectHighlightsUtil.getElementsInRange(
      psiFile,
      0,
      psiFile.getTextLength).asScala.toList

    allPsiElements.collect {
      case e @ WeaverTestMethod() => position(e, document)
    }.mkString("\n")
  }

  private def addModuleDependencies(): Unit = {
    val module = ModuleManager.getInstance(myProject).getModules()(0)
    IvyManagedLoader(DependencyDescription(
      "com.disneystreaming",
      "weaver-core_2.13",
      weaver.build.BuildInfo.version
    )).init(module, myTestRootDisposable)
  }

  private def position(element: PsiElement, document: Document): String = {
    val line: Int = document.getLineNumber(element.getTextRange.getStartOffset)
    val column: Int =
      element.getTextRange.getStartOffset - document.getLineStartOffset(line)
    val label = element.getText
    s"$label-${line + 1}:${column + 1}"
  }
}

object WeaverTestMethodTest extends TestSuiteCompanion[WeaverTestMethodTest]
