package weaver

import scala.scalajs.js
import js.annotation._

private[weaver] object FSCompat {

  @js.native
  @JSGlobal("process")
  object Process extends js.Object {
    def cwd(): String = js.native
  }

  @js.native
  @JSImport("path", JSImport.Namespace)
  object Path extends js.Object {
    def resolve(s: String): String = js.native
  }

  def wd: String = Process.cwd()

  def bestEffortPath(
      fileName: Option[String],
      filePath: Option[String]): Option[String] = {
    val pwd = wd + "/"

    filePath.flatMap { pathString =>
      val path = Path.resolve(pathString)
      if (path.startsWith(pwd)) Some(path.stripPrefix(pwd))
      else fileName
    }
  }
}
