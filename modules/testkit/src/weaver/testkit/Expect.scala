package weaver.testkit

import com.eed3si9n.expecty._

import cats.data.ValidatedNel
import cats.implicits._
import weaver.SourceLocation

case class Expectation(run: ValidatedNel[String, Unit]) {
  def and(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromExpect(this).and(other)
  def or(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromExpect(this).or(other)
}

class Expect extends Recorder[Boolean, Expectation] {

  class ExpectyListener extends RecorderListener[Boolean, Expectation] {
    override def expressionRecorded(
        recordedExpr: RecordedExpression[Boolean],
        recordedMessage: Function0[String]): Unit = {}

    override def recordingCompleted(
        recording: Recording[Boolean],
        recordedMessage: Function0[String]): Expectation = {
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
      Expectation(res)
    }
  }

  override lazy val listener = new ExpectyListener
}
