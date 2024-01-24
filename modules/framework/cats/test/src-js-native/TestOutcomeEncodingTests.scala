package weaver
package framework
package test

object TestOutcomeEncodingTests
    extends FunSuite {

  private val anOutcome =
    TestOutcomeNative("suite-1", "test-1", 10, "verbosity goes here")

  roundtripTest("even, small doubles for the duration",
                anOutcome.copy(durationMs = 42))

  roundtripTest("bigger doubles for the duration",
                anOutcome.copy(durationMs = 50000))

  roundtripTest("uneven doubles for the duration",
                anOutcome.copy(durationMs = 42.57))

  roundtripTest(
    "Int.MaxValue duration",
    anOutcome.copy(durationMs = Int.MaxValue)
  )
  roundtripTest(
    "Double.MaxValue duration",
    anOutcome.copy(durationMs = Double.MaxValue)
  )

  private def roundtripTest(name: TestName, input: TestOutcomeNative)(implicit
  loc: SourceLocation) = {

    test(name.copy(name = "Roundtrip test - " + name.name)) {
      val encoded = TestOutcomeNative.encode(input)
      val decoded = TestOutcomeNative.decode(encoded)

      assert.same(input, decoded)
    }
  }

}
