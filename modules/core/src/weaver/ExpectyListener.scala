package weaver

import com.eed3si9n.expecty._
import cats.data.{ NonEmptyList, ValidatedNel }
import cats.syntax.all._


class ExpectyListener extends RecorderListener[Boolean, Expectations] {
    def sourceLocation(loc: Location): SourceLocation = {
      SourceLocation(loc.path, loc.relativePath, loc.line)
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
            val sourceLocs  = NonEmptyList.of(sourceLoc)
            new AssertionException(fullMessage, sourceLocs).invalidNel
          } else ().validNel
      }
      Expectations(res)
    }
  }