package weaver

import com.eed3si9n.expecty._

import cats.data.ValidatedNel
import cats.implicits._

case class SingleExpectation(run: ValidatedNel[String, Unit]) {
  def and(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).and(other)
  def or(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).or(other)
  def toExpectations(implicit loc: SourceLocation) =
    Expectations.fromSingle(this)
}

class Expect extends Recorder[Boolean, SingleExpectation] {

  class ExpectyListener extends RecorderListener[Boolean, SingleExpectation] {
    override def expressionRecorded(
        recordedExpr: RecordedExpression[Boolean],
        recordedMessage: Function0[String]): Unit = {}

    override def recordingCompleted(
        recording: Recording[Boolean],
        recordedMessage: Function0[String]): SingleExpectation = {
      val res = recording.recordedExprs.foldMap[ValidatedNel[String, Unit]] {
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
            fullMessage.invalidNel
          } else ().validNel
      }
      SingleExpectation(res)
    }
  }

  override lazy val listener = new ExpectyListener
}
