package weaver

import internals._

import com.eed3si9n.expecty._
import cats.kernel.Eq
import cats.Show
import cats.data.Validated
import cats.data.NonEmptyList

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

  def sameAs[A](expected: PartialWrapper[A]): PartiallyAppliedExpectEqual[A] =
    new PartiallyAppliedExpectEqual(expected.expected,
                                    expected.eqA,
                                    expected.showA)
}

// workaround for implicit parameters getting in the way of calling `apply`
case class PartialWrapper[A](expected: A, eqA: Eq[A], showA: Show[A])

object PartialWrapper {
  implicit def fromA[A](a: A)(
      implicit eqA: Eq[A] = Eq.fromUniversalEquals[A],
      showA: Show[A] = Show.fromToString[A]) =
    new PartialWrapper[A](a, eqA, showA)
}

class PartiallyAppliedExpectEqual[A](expected: A, eqA: Eq[A], showA: Show[A])
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
                    header + "\n\n" + diff,
                    sourceLocs)))
            }
          case _ => throw new RuntimeException(
              "unexpected number of expressions " + recording)
        }
      }
    }
}
