package weaver

/**
 * Exactly the same as SourceLocation, but typed differently to specifically
 * location of test declaration, for tooling purposes.
 */
final case class TestLocation(
    fileName: Option[String],
    filePath: Option[String],
    line: Int
)

object TestLocation {
  implicit def fromContext(implicit
  sourceLocation: SourceLocation): TestLocation =
    TestLocation(sourceLocation.fileName,
                  sourceLocation.filePath,
                  sourceLocation.line)
}
