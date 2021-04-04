package weaver

/**
 * An identifier to a test in a suite.
 *
 * The implicit conversion from String is used as a mean
 * for IDEs to detect the location of individual tests.
 */
case class TestName(name: String, location: SourceLocation, tags: Set[String]) {
  def tagged(str: String): TestName = this.copy(tags = tags + str)
  def only: TestName                = tagged("only")
}

object TestName {
  implicit def fromString(s: String)(
      implicit location: SourceLocation): TestName =
    TestName(s, location, Set.empty)
}
