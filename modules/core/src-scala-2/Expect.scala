package weaver

// import cats.data.{ NonEmptyList, ValidatedNel }
// import cats.syntax.all._

import com.eed3si9n.expecty._

class Expect
    extends Recorder[Boolean, Expectations]
    with UnaryRecorder[Boolean, Expectations] {

  def all(recordings: Boolean*): Expectations =
    macro VarargsRecorderMacro.apply[Boolean, Expectations]

  override lazy val listener = new ExpectyListener
}
