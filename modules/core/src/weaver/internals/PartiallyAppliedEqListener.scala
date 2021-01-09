package weaver
package internals

import cats.Show
import cats.data.{ NonEmptyList, Validated }
import cats.kernel.Eq

import com.eed3si9n.expecty._

private[weaver] class PartiallyAppliedEqCheck[A](
    expected: A,
    eqA: Eq[A],
    showA: Show[A])
    extends Recorder[A, Expectations]
    with UnaryRecorder[A, Expectations] {

  def sourceLocation(loc: Location): SourceLocation = {
    SourceLocation(loc.path, loc.relativePath, loc.line)
  }

  override lazy val listener: RecorderListener[A, Expectations] =
    new RecorderListener[A, Expectations] {

      def sourceLocation(loc: Location): SourceLocation = {
        SourceLocation(loc.path, loc.relativePath, loc.line)
      }
      def recordingCompleted(
          recording: Recording[A],
          recordedMessage: () => String): Expectations = {
        recording.recordedExprs match {
          case found :: Nil =>
            lazy val rendering: String = found.text.trim()

            if (eqA.eqv(expected, found.value))
              Expectations(Validated.validNel(()))
            else {
              val header = "assertion failed: "

              val expectedLines = showA.show(expected).linesIterator.toSeq
              val foundLines    = showA.show(found.value).linesIterator.toSeq
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
                    header + "\n\n" + rendering + "\n\n" + diff,
                    sourceLocs)))
            }
          case _ => throw new RuntimeException(
              "unexpected number of expressions " + recording)
        }
      }
    }
}
