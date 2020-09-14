package weaver.intellij.testsupport

import com.intellij.psi.PsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr._

/**
 * This attempts to detect an implicit conversion from
 * String to TestName in order, the position of which will
 * be captured by the implicit conversion.
 */
case object WeaverTestMethod {

  def unapply(e: PsiElement): Boolean = {
    e match {
      case sce: ScExpression =>
        val isFromString = sce.implicitConversion().map(
          _.element.getName()).contains("fromString")
        val isTestName = sce.getTypeAfterImplicitConversion().tr.map(
          _.canonicalText).contains("_root_.weaver.TestName")
        isFromString && isTestName
      case _ => false
    }
  }
}
