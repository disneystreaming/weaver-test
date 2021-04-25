package weaver.specs2compat

import weaver.{ Expectations, SimpleIOSuite }

import org.specs2.matcher.MatchResult

object MatchersSpec extends SimpleIOSuite with IOMatchers {
  pureTest("pureTest { 1 must beEqualTo(1) }") {
    1 must beEqualTo(1)
  }

  pureTest("pureTest { 1 must be_==(1) }") {
    1 must be_==(1)
  }

  pureTest("pureTest { 1 must_== 1 }") {
    1 must_== 1
  }

  pureTest("pureTest { 1 mustEqual 1 }") {
    1 mustEqual 1
  }

  pureTest("pureTest { 1 === 1 }") {
    1 === 1
  }

  pureTest("pureTest { (1 === 1) and (1 === 1) }") {
    (1 === 1) and (1 === 1)
  }

  pureTest("pureTest { (1 === 1) or (1 === 1) }") {
    (1 === 1) or (1 === 1)
  }

  pureTest("pureTest { Some(1) must beSome(1) }") {
    Some(1) must beSome(1)
  }

  pureTest("pureTest { Some(1) must beSome((i: Int) => i === 1) }") {
    Some(1) must beSome((i: Int) => i === 1)
  }

  pureTest("deal with 'and' inside 'beSome' check") {
    Some(1) must beSome((i: Int) => (i === 1) and (i === 1))
  }

  pureTest("deal with 'or' inside 'beSome' check") {
    Some(1) must beSome((i: Int) => (i === 1) or (i === 1))
  }

  pureTest("deal with nested beSome matchers") {
    Some(1) must beSome((i: Int) => {
      (i === 1) and (Some(1) must beSome((i: Int) => (i === 1) and (i === 1)))
    })
  }

  pureTest("pureTest { 1 must beLike { case i: Int => i === 1 } }") {
    1 must beLike { case i: Int => i === 1 }
  }

  pureTest(
    "pureTest { 1 must beLike { case i: Int => (i === 1) and (i === 1) } }") {
    1 must beLike { case i: Int => (i === 1) and (i === 1) }
  }

  def expectFailure[A](matchResult: MatchResult[A]): Expectations = {
    matchResult.run.toEither.fold(
      nel => expect(nel.head.message == matchResult.toResult.message),
      _ => failure("Expected assertion exception")
    )
  }

  pureTest("pureTest { expectFailure { 1 must beEqualTo(2) } }") {
    expectFailure { 1 must beEqualTo(2) }
  }

  pureTest("pureTest { expectFailure { 1 must be_==(2) } }") {
    expectFailure(1 must be_==(2))
  }

  pureTest("pureTest { expectFailure { 1 must_== 2 } }") {
    expectFailure(1 must_== 2)
  }

  pureTest("pureTest { expectFailure { 1 mustEqual 2 } }") {
    expectFailure { 1 mustEqual 2 }
  }

  pureTest("pureTest { expectFailure { 1 === 2 } }") {
    expectFailure(1 === 2)
  }

  pureTest("pureTest { expectFailure { Some(1) must beSome((i: Int) => (i === 1) and (i === 2)) } }") {
    val matchResult = Some(1) must beSome((i: Int) => (i === 1) and (i === 2))
    expectFailure(matchResult) &&
    expect(matchResult.message.contains("Some(1) is Some but 1 != 2"))
  }
}
