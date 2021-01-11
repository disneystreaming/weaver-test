package weaver
package internals

import cats.Show
import cats.data.{ NonEmptyList, Validated }
import cats.kernel.Eq

import com.eed3si9n.expecty._

private[weaver] trait ExpectSame {

  def same[A](
      expected: A,
      found: A)(
      implicit eqA: Eq[A] = Eq.fromUniversalEquals[A],
      showA: Show[A] = Show.fromToString[A],
      loc: SourceLocation): Expectations = {

    if (eqA.eqv(expected, found))
      Expectations(Validated.validNel(()))
    else {
      val header = "Values not equal:"

      val expectedLines = showA.show(expected).linesIterator.toSeq
      val foundLines    = showA.show(found).linesIterator.toSeq
      val sourceLocs    = NonEmptyList.of(loc)
      val diff = DiffUtil
        .mkColoredLineDiff(expectedLines, foundLines)
        .linesIterator
        .toSeq
        .map(str => Console.RESET.toString + str)
        .mkString("\n")

      Expectations(
        Validated.invalidNel[AssertionException, Unit](
          new AssertionException(header + "\n\n" + diff, sourceLocs)))
    }
  }
}
