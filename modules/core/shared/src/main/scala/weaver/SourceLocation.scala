package weaver

// kudos to https://github.com/monix/minitest
final case class SourceLocation(
    filePath: String,
    fileRelativePath: String,
    line: Int
) {
  def fileName: Option[String] = filePath.split("/").lastOption
}

object SourceLocation extends SourceLocationMacro
