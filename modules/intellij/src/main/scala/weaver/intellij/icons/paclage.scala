package weaver.intellij.icons

import com.intellij.ui.IconManager
import javax.swing.Icon

package object icons {
  private def load(path: String): Icon =
    IconManager.getInstance.getIcon(path, getClass)

  val SortByIdIcon: Icon = load("/logo.png")
}
