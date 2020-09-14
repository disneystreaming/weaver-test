package weaver

/**
 * An identifier to a test in a suite.
 *
 * The implicit conversion from String is used as a mean
 * for IDEs to detect the location of individual tests.
 *
 * The IDE is then able to request running a test at a specific
 * location, using a CLI call. The framework will be able to lookup
 * the test using the location (which should match with what the IDE
 * knows)
 */
case class TestName(name: String, location: SourceLocation)

object TestName {
  implicit def fromString(s: String)(
      implicit location: SourceLocation): TestName =
    TestName(s, location)
}
