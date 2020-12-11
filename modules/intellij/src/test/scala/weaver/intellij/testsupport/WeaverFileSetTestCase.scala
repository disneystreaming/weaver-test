package weaver.intellij.testsupport

import java.io.File

import com.intellij.FileSetTestCase
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.psi.{ PsiDocumentManager, PsiFileFactory }
import com.intellij.util.LocalTimeCounter
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaFileImpl

abstract class WeaverFileSetTestCase(subpath: String)
    extends FileSetTestCase(WeaverFileSetTestCase.rootPath(subpath)) {

  override final def transform(
      testName: String,
      data: Array[String]): String = {
    transform(data.toSeq.map(_.stripLineEnd))
  }

  protected def transform(data: Seq[String]): String

  def createPseudoPhysicalScalaFile(text: String): ScalaFileImpl = {
    val project  = myProject
    val tempFile = project.getBasePath + "/temp." + "scala"
    val fileType = FileTypeManager.getInstance.getFileTypeByFileName(tempFile)
    PsiFileFactory.getInstance(project)
      .createFileFromText(tempFile,
                          fileType,
                          text,
                          LocalTimeCounter.currentTime(),
                          true)
  }.asInstanceOf[ScalaFileImpl]

  def getDocument(psiFile: ScalaFileImpl): Document = {
    PsiDocumentManager.getInstance(myProject).getDocument(psiFile)
  }

  override def getName: String = getClass.getName
}

object WeaverFileSetTestCase {
  System.setProperty("fileset.pattern", "(.*)\\.test")

  def rootPath(subpath: String): String = {
    val testdataPath: String = {
      // SBT changes the pwd to the module's directory when executing the tests
      // or the main on a forked jvm.
      val unforkedJVMFile = new File("./modules/intellij/testdata")
      if (unforkedJVMFile.exists()) unforkedJVMFile.getAbsolutePath()
      else new File("testdata").getAbsolutePath()
    }

    s"$testdataPath/$subpath"
  }
}

class TestDisposable extends Disposable {
  private var myDisposed: Boolean = false

  override def dispose(): Unit = {
    myDisposed = true;
  }
  def isDisposed: Boolean = myDisposed
}
