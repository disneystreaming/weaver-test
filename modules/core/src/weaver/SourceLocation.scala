package weaver

// kudos to https://github.com/monix/minitest
// format: off
import scala.reflect.macros.whitebox
import scala.util.{Try => STry}

final case class SourceLocation(
  fileName: Option[String],
  filePath: Option[String],
  fileRelativePath: Option[String],
  line: Int
)

object SourceLocation {
  implicit def fromContext: SourceLocation =
    macro Macros.fromContext

  class Macros(val c: whitebox.Context) {
    import c.universe._

    def fromContext: Tree = {
      val (fileNameExpr, pathExpr, relPathExpr, lineExpr) = getSourceLocation
      val SourceLocationSym = symbolOf[SourceLocation].companion
      q"""$SourceLocationSym($fileNameExpr, $pathExpr, $relPathExpr, $lineExpr)"""
    }

    private def getSourceLocation = {
      val pwd  = java.nio.file.Paths.get("").toAbsolutePath
      val file = STry(Option(c.enclosingPosition.source.file.file)).toOption.flatten
      val path = file.map(_.getPath)
      val relativePath = file.map { f =>
        pwd.relativize(f.toPath()).toString()
      }

      val line = c.Expr[Int](Literal(Constant(c.enclosingPosition.line)))
      (wrapOption(file.map(_.getName)), wrapOption(path), wrapOption(relativePath), line)
    }

    private def wrapOption[A](opt: Option[A]): c.Expr[Option[A]] =
      c.Expr[Option[A]](
        opt match {
          case None =>
            q"""_root_.scala.None"""
          case Some(value) =>
            val v = c.Expr[A](Literal(Constant(value)))
            q"""_root_.scala.Option($v)"""
        })
  }
}
// format: on
