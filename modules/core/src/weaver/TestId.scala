package weaver

/**
 * An identifier to a test in a suite.
 *
 * The test location allows UI/editors to display some graphic element for users
 * to run specific tests within a suite.
 *
 * It is implicitly converted from a suite via macro capture.
 */
case class TestId(name: String, location: TestLocation)

object TestId {
  implicit def fromString(s: String)(implicit location: TestLocation): TestId =
    TestId(s, location)
}
