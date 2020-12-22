package weaver

// kudos to https://github.com/monix/minitest
// format: off
// import scala.reflect.macros.whitebox

final case class SourceLocation(
  filePath: String,
  fileRelativePath: String,
  line: Int
){
  def fileName : Option[String] = filePath.split("/").lastOption
}

object SourceLocation extends SourceLocationMacro

//   trait Here {
//     /**
//       * Pulls source location without being affected by implicit scope.
//       */
//     def here: SourceLocation = macro Macros.fromContext
//   }

//   implicit def fromContext: SourceLocation =
//     macro Macros.fromContext

//   class Macros(val c: whitebox.Context) {
//     import c.universe._

//     def fromContext: Tree = {
//       val (pathExpr, relPathExpr, lineExpr) = getSourceLocation
//       val SourceLocationSym = symbolOf[SourceLocation].companion
//       q"""$SourceLocationSym($pathExpr, $relPathExpr, $lineExpr)"""
//     }

//     private def getSourceLocation = {
//       val pwd  = java.nio.file.Paths.get("").toAbsolutePath
//       val p = c.enclosingPosition.source.path
//       val abstractFile = c.enclosingPosition.source.file

//       val rp = if (!abstractFile.isVirtual){
//         pwd.relativize(abstractFile.file.toPath()).toString()
//       } else p

//       val line = c.Expr[Int](Literal(Constant(c.enclosingPosition.line)))
//       (p, rp, line)
//     }

//   }
// }
// format: on
