package weaver

import com.eed3si9n.expecty._

import internals._

class Expect
    extends Recorder[Boolean, Expectations]
    with UnaryRecorder[Boolean, Expectations] {

  def all(recordings: Boolean*): Expectations =
    macro VarargsRecorderMacro.apply[Boolean, Expectations]

  def sameString(expected: String, found: String): Expectations =
    macro StringRecorderMacro.apply[String, Expectations]

  override lazy val listener: RecorderListener[Boolean, Expectations] =
    new ExpectyListener

  lazy val stringAssertEqualsListener: RecorderListener[String, Expectations] =
    new StringAssertEqualsListener

}
