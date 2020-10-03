package weaver

import cats.syntax.all._
import cats.data.ValidatedNel

import com.eed3si9n.expecty._

class Expect
    extends Recorder[Boolean, Expectations]
    with UnaryRecorder[Boolean, Expectations] {

  def all(recordings: Boolean*): Expectations =
    macro VarargsRecorderMacro.apply[Boolean, Expectations]

  class ExpectyListener extends RecorderListener[Boolean, Expectations] {
    def sourceLocation(loc: Location): SourceLocation = {
      val name = loc.path.split("/").lastOption
      SourceLocation(name, Some(loc.path), Some(loc.relativePath), loc.line)
    }

    override def expressionRecorded(
        recordedExpr: RecordedExpression[Boolean],
        recordedMessage: Function0[String]): Unit = {}

    override def recordingCompleted(
        recording: Recording[Boolean],
        recordedMessage: Function0[String]): Expectations = {
      type Exp = ValidatedNel[AssertionException, Unit]
      val res = recording.recordedExprs.foldMap[Exp] {
        expr =>
          lazy val rendering: String =
            new ExpressionRenderer(showTypes = false).render(expr)

          if (!expr.value) {
            val msg = recordedMessage()
            val header =
              "assertion failed" +
                (if (msg == "") ""
                 else ": " + msg)
            val fullMessage = header + "\n\n" + rendering
            val sourceLoc   = sourceLocation(expr.location)
            new AssertionException(fullMessage, sourceLoc).invalidNel
          } else ().validNel
      }
      Expectations(res)
    }
  }

  override lazy val listener = new ExpectyListener
}
