package weaver

import com.eed3si9n.expecty._
import internals._

import scala.quoted._

class Expect
    extends Recorder[Boolean, Expectations]
    with UnaryRecorder[Boolean, Expectations]
    with ExpectSame {

  inline def all(inline recordings: Boolean*): Expectations =
    ${ RecorderMacro.varargs('recordings, 'listener) }

  override lazy val listener = new ExpectyListener

}
