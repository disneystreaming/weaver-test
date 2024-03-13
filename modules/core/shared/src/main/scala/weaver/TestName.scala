package weaver

/**
 * An identifier to a test in a suite.
 *
 * The implicit conversion from String is used as a mean for IDEs to detect the
 * location of individual tests.
 */
case class TestName(name: String, location: SourceLocation, tags: Set[String]) {
  def tagged(str: String): TestName = this.copy(tags = tags + str)
  def only: TestName                = tagged(TestName.Tags.only)
  def ignore: TestName              = tagged(TestName.Tags.ignore)
}

object TestName {
  implicit def fromString(s: String)(
      implicit location: SourceLocation): TestName =
    TestName(s, location, Set.empty)

  object Tags {
    val only   = "only"
    val ignore = "ignore"
  }
}
