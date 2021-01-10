package weaver
package framework
package test

import cats.kernel.Eq

object ExpectationsTests extends SimpleIOSuite {

  pureTest("and") {
    expect(1 == 1) and expect(2 == 2) && not(expect(1 == 2))
  }

  pureTest("or") {
    (expect(1 == 1) or expect(2 == 1)) &&
    (expect(2 == 1) or expect(1 == 1)) &&
    not(expect(2 == 1) || expect(1 == 2))
  }

  pureTest("xor") {
    (expect(1 == 1) xor expect(2 == 1)) &&
    (expect(2 == 1) xor expect(1 == 1)) &&
    not(expect(1 == 1) xor expect(2 == 2)) &&
    not(expect(2 == 1) xor expect(1 == 2))
  }

  pureTest("all") {
    expect.all(
      1 == 1,
      "a" + "b" == "ab",
      true || false
    )
  }

  pureTest("forall (success)") {
    forEach(List(true, true))(value => expect(value == true))
  }

  pureTest("forall (failure)") {
    not(forEach(List(true, false))(value => expect(value == true)))
  }

  pureTest("equality check") {
    expect.same("foo", "foo") and
      not(expect.same("bar", "foo"))
  }

  pureTest("expect.same respects cats.kernel.Eq") {
    implicit val eqInt: Eq[Int] = Eq.allEqual
    expect.same(0, 1)
  }

}
