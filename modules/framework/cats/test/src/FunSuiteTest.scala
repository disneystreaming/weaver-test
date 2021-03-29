package weaver
package framework
package test

import cats.kernel.Eq

object FunSuiteTest extends FunSuite {

  test("and") {
    expect(1 == 1) and expect(2 == 2) && not(expect(1 == 2))
  }

  test("or") {
    (expect(1 == 1) or expect(2 == 1)) &&
    (expect(2 == 1) or expect(1 == 1)) &&
    not(expect(2 == 1) || expect(1 == 2))
  }

  test("xor") {
    (expect(1 == 1) xor expect(2 == 1)) &&
    (expect(2 == 1) xor expect(1 == 1)) &&
    not(expect(1 == 1) xor expect(2 == 2)) &&
    not(expect(2 == 1) xor expect(1 == 2))
  }

  test("all") {
    expect.all(
      1 == 1,
      "a" + "b" == "ab",
      true || false
    )
  }

  test("forall (success)") {
    forEach(List(true, true))(value => expect(value == true))
  }

  test("forall (failure)") {
    not(forEach(List(true, false))(value => expect(value == true)))
  }

  test("equality check") {
    expect.same("foo", "foo") and
      not(expect.same("bar", "foo"))
  }

  test("expect.same respects cats.kernel.Eq") {
    implicit val eqInt: Eq[Int] = Eq.allEqual
    expect.same(0, 1)
  }

}
