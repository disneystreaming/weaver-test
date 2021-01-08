package weaver

import cats.data.{ NonEmptyList, ValidatedNel }
import cats.syntax.all._

import com.eed3si9n.expecty._

import scala.quoted._

class Expect
    extends Recorder[Boolean, Expectations]
    with UnaryRecorder[Boolean, Expectations] {

  inline def all(inline recordings: Boolean*): Expectations =
    ${ RecorderMacro.varargs('recordings, 'listener) }

  inline def sameString(expected: String, found: String) : Expectations =
    ${ StringRecorderMacro.apply('expected, 'found, 'stringAssertEqualsListener)}

  override lazy val listener = new ExpectyListener

  lazy val stringAssertEqualsListener: RecorderListener[String, Expectations] =
    new StringAssertEqualsListener
}
