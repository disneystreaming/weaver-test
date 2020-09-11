package weaver

import cats.data.Validated
import cats.data.Validated.{ Invalid, Valid }
import cats.effect.Sync
import cats.syntax.all._

import com.eed3si9n.expecty._

case class SingleExpectation(run: Validated[String, Unit]) {
  def and(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).and(other)
  def &&(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).and(other)
  def or(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).or(other)
  def ||(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).or(other)
  def xor(other: Expectations)(implicit loc: SourceLocation) =
    Expectations.fromSingle(this).xor(other)

  def toExpectations(implicit loc: SourceLocation) =
    Expectations.fromSingle(this)

  def failFast[F[_]: Sync](implicit loc: SourceLocation): F[Unit] =
    this.run match {
      case Invalid(e) => Sync[F].raiseError(new AssertionException(e, loc))
      case Valid(_)   => Sync[F].unit
    }
}

class Expect extends Recorder[Boolean, SingleExpectation] {

  class ExpectyListener extends RecorderListener[Boolean, SingleExpectation] {
    override def expressionRecorded(
        recordedExpr: RecordedExpression[Boolean],
        recordedMessage: Function0[String]): Unit = {}

    override def recordingCompleted(
        recording: Recording[Boolean],
        recordedMessage: Function0[String]): SingleExpectation = {
      val res = recording.recordedExprs.foldMap[Validated[String, Unit]] {
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
            fullMessage.invalid
          } else ().valid
      }
      SingleExpectation(res)
    }
  }

  override lazy val listener = new ExpectyListener
}
