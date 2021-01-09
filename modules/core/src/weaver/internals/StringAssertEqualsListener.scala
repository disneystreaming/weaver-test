package weaver

import cats.data.{ NonEmptyList, Validated }

import com.eed3si9n.expecty._

class StringAssertEqualsListener[A]
    extends RecorderListener[String, Expectations] {

  def sourceLocation(loc: Location): SourceLocation = {
    SourceLocation(loc.path, loc.relativePath, loc.line)
  }

  override def recordingCompleted(
      recording: Recording[String],
      recordedMessage: Function0[String]) = {
    recording.recordedExprs match {
      case found :: expected :: Nil =>
        if (expected.value == found.value) Expectations(Validated.validNel(()))
        else {
          lazy val rendering: String = new ExpressionRenderer(
            showTypes = false,
            shortString = true).render(found)
          val msg = recordedMessage()
          val header =
            "assertion failed" +
              (if (msg == "") ""
               else ": " + msg)

          val expectedLines = expected.value.linesIterator.toSeq
          val foundLines    = found.value.linesIterator.toSeq
          val sourceLoc     = sourceLocation(found.location)
          val sourceLocs    = NonEmptyList.of(sourceLoc)
          val diff = DiffUtil
            .mkColoredLineDiff(expectedLines, foundLines)
            .linesIterator
            .toSeq
            .map(str => Console.RESET.toString + str)
            .mkString("\n")

          Expectations(
            Validated.invalidNel[AssertionException, Unit](
              new AssertionException(
                header + "\n\n" + rendering + diff,
                sourceLocs)))
        }
      case _ => throw new RuntimeException(
          "unexpected number of expressions " + recording)
    }
  }
}
