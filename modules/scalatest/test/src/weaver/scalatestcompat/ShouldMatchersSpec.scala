package weaver.scalatestcompat

import weaver.SimpleIOSuite

object ShouldMatchersSpec extends SimpleIOSuite with IOShouldMatchers {
  pureTest("pureTest { 1 shouldBe 1 }") {
    1 shouldBe 1
  }

  pureTest("pureTest { 1 should be (1) }") {
    1 should be(1)
  }

  pureTest("pureTest { 1 shouldEqual 1 }") {
    1 shouldEqual 1
  }

  pureTest("pureTest { 1 should equal (1) }") {
    1 should equal(1)
  }

  pureTest("pureTest { 1 should === (1) }") {
    1 should ===(1)
  }

  pureTest("pureTest { expectFailure { 1 shouldBe 2 } }") {
    expectFailure { 1 shouldBe 2 }
  }

  pureTest("pureTest { expectFailure {1 should be (2) } }") {
    expectFailure { 1 should be(2) }
  }

  pureTest("pureTest { expectFailure { 1 shouldEqual 2 } }") {
    expectFailure { 1 shouldEqual 2 }
  }

  pureTest("pureTest { expectFailure { 1 should equal (2) } }") {
    expectFailure { 1 should equal(2) }
  }

  pureTest("pureTest { expectFailure { 1 should === (2) } }") {
    expectFailure { 1 should ===(2) }
  }
}
