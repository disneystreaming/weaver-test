package org.jetbrains.plugins.scala.testingSupport.test

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction

case object WeaverTestMethod {
  def unapply(e: PsiElement): Boolean = {
    e match {
      case e: LeafPsiElement =>
        Option(e.getParent) match {
          case Some(ScReferenceExpression(definition)) =>
            definition match {
              case f: ScFunction =>
                f.clauses.exists(_.params.exists {
                  _.`type`().exists(
                    _.canonicalText == "_root_.weaver.TestIndicator")
                })
              case _ => false
            }
          case _ => false
        }
      case _ => false
    }
  }
}
