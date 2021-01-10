package weaver

import com.eed3si9n.expecty._

import internals._

class Expect
    extends Recorder[Boolean, Expectations]
    with UnaryRecorder[Boolean, Expectations]
    with ExpectSame {

  def all(recordings: Boolean*): Expectations =
    macro VarargsRecorderMacro.apply[Boolean, Expectations]

  override lazy val listener: RecorderListener[Boolean, Expectations] =
    new ExpectyListener

}
