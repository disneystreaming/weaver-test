package weaver

/**
 * Exactly the same as SourceLocation, but typed differently to specifically
 * location of test declaration, for tooling purposes.
 */
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
