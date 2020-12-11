package weaver.intellij.testsupport

import scala.collection.mutable

import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.plugins.scala.DependencyManagerBase.{
  DependencyDescription,
  ResolvedDependency
}
import org.jetbrains.plugins.scala.{ DependencyManager, DependencyManagerBase }

case class IvyManagedLoader(dependencies: DependencyDescription*) {
  protected lazy val dependencyManager: DependencyManagerBase =
    DependencyManager

  def init(implicit module: Module, disposable: Disposable): Unit = {
    val resolved = IvyManagedLoader.cache.getOrElseUpdate(
      dependencies,
      dependencyManager.resolve(dependencies: _*)
    )
    resolved.foreach { resolved =>
      VfsRootAccess.allowRootAccess(disposable, resolved.file.getCanonicalPath)
      PsiTestUtil.addLibrary(disposable,
                             module,
                             resolved.info.toString,
                             resolved.file.getParent,
                             resolved.file.getName)
    }
  }
}

object IvyManagedLoader {

  private val cache: mutable.Map[Seq[DependencyDescription], Seq[ResolvedDependency]] =
    mutable.Map()
}
