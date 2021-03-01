package weaver

import org.scalatest.Assertion
import org.scalatest.exceptions.TestFailedException
import scala.util.Try
import Expectations.Helpers._

package object scalatestcompat {
  def expectFailure(assertion: => Assertion): Expectations = {
    Try(assertion).fold(
      t => expect(t.isInstanceOf[TestFailedException]),
      _ => failure("Expected assertion exception")
    )
  }
}
