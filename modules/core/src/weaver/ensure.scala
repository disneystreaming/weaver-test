package weaver

import cats.Show
import cats.kernel.Eq

class ensure[A](value: A, eqA: Eq[A], showA: Show[A]) {
  def is: internals.PartiallyAppliedEqCheck[A] =
    new internals.PartiallyAppliedEqCheck[A](value, eqA, showA)
}

object ensure {
  def apply[A](a: A)(
      implicit eqA: Eq[A] = Eq.fromUniversalEquals[A],
      showA: Show[A] = Show.fromToString[A]): ensure[A] =
    new ensure[A](a, eqA, showA)
}
