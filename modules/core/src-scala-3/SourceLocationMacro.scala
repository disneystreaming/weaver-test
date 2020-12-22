package weaver

// kudos to https://github.com/monix/minitest
// format: off
// import scala.reflect.macros.whitebox

import scala.quoted._

trait SourceLocationMacro {
  trait Here {
    /**
      * Pulls source location without being affected by implicit scope.
      */
    inline def here: SourceLocation = ${macros.fromContextImpl}
  }

  implicit inline def fromContext: SourceLocation = ${macros.fromContextImpl}
}

object macros {
  def fromContextImpl(using ctx: Quotes): Expr[SourceLocation] = {
    import ctx.reflect.Position._
    import ctx.reflect._

    val pwd  = java.nio.file.Paths.get("").toAbsolutePath

    val position = Position.ofMacroExpansion

    val rp = Expr(pwd.relativize(position.sourceFile.jpath).toString)
    val absPath = Expr(position.sourceFile.jpath.toAbsolutePath.toString)
    val l = Expr(position.startLine + 1)

    '{new SourceLocation($absPath, $rp, $l) }
  }
}

// format: on
