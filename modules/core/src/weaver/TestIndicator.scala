package weaver

final case class TestIndicator(
    fileName: Option[String],
    filePath: Option[String],
    line: Int
)

object TestIndicator {
  implicit def fromContext(implicit
  sourceLocation: SourceLocation): TestIndicator =
    TestIndicator(sourceLocation.fileName,
                  sourceLocation.filePath,
                  sourceLocation.line)
}
