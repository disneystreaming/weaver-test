package weaver
package framework
package test

import cats.Show

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

  pureTest("string comparison") {
    expect.sameString("foo", "foo") and
      not(expect.sameString("bar", "foo"))
  }

  pureTest("case class comparison") {
    case class Foo(s: String, i: Int)
    object Foo {
      implicit val show = Show.show[Foo] {
        case Foo(s, i) =>
          s"""
          |Foo {
          |  s: ${Show[String].show(s)}
          |  i: ${Show[Int].show(i)}
          |}
          """.stripMargin.trim()
      }
    }

    expect.sameAs(Foo("foo", 1))(Foo("foo", 2))
  }
}
