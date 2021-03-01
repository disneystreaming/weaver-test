package weaver.scalatestcompat

import weaver.SimpleIOSuite

object MustMatchersSpec extends SimpleIOSuite with IOMustMatchers {
  pureTest("pureTest { 1 mustBe 1 }") {
    1 mustBe 1
  }

  pureTest("pureTest { 1 must be (1) }") {
    1 must be(1)
  }

  pureTest("pureTest { 1 mustEqual 1 }") {
    1 mustEqual 1
  }

  pureTest("pureTest { 1 must equal (1) }") {
    1 must equal(1)
  }

  pureTest("pureTest { 1 must === (1) }") {
    1 must ===(1)
  }

  pureTest("pureTest { expectFailure { 1 mustBe 2 } }") {
    expectFailure { 1 mustBe 2 }
  }

  pureTest("pureTest { expectFailure { 1 must be (2) } }") {
    expectFailure { 1 must be(2) }
  }

  pureTest("pureTest { expectFailure { 1 mustEqual 2 } }") {
    expectFailure { 1 mustEqual 2 }
  }

  pureTest("pureTest { expectFailure { 1 must equal (2) } }") {
    expectFailure { 1 must equal(2) }
  }

  pureTest("pureTest { expectFailure { 1 must === (2) } }") {
    expectFailure { 1 must ===(2) }
  }
}
