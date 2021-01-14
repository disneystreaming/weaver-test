---
id: version-0.6.0-M5-expectations
title: Expectations (assertions)
original_id: expectations
---

Expectations are pure, composable values. This forces developers to separate the test's checks from the scenario, which is generally cleaner/clearer.

The easiest way to construct expectactions is to call the `expect` macro, which is built using the [expecty](https://github.com/eed3si9n/expecty/) library.

```scala
import weaver._
import cats.effect.IO

object MySuite2 extends SimpleIOSuite {

  pureTest("And/Or composition") {
    expect(1 != 2) and expect(2 != 1) or expect(2 != 3)
  }

  pureTest("Varargs composition") {
    // expect(1 + 1 == 2) && expect (2 + 2 == 4) && expect(4 * 2 == 8)
    expect.all(1 + 1 == 2, 2 + 2 == 4, 4 * 2 == 8)
  }

  pureTest("Pretty string diffs") {
    expect.same("foo", "bar")
  }

  pureTest("Foldable operations") {
    val list = List(1,2,3)
    import cats.instances.list._
    forEach(list)(i => expect(i > 0)) and
    exists(list)(i => expect(i == 3))
  }

  pureTest("Non macro-based expectations") {
    val condition : Boolean = false
    if (condition) success else failure("Condition failed")
  }

  test("Failing fast expectations") {
    for {
      h <- IO.pure("hello")
      _ <- expect(h.nonEmpty).failFast
    } yield success
  }

}
```

## Tracing locations of failed expectations

As of 0.5.0, failed expectations carry a `NonEmptyList[SourceLocation]`, which can be used to manually trace the callsites that lead to a failure.

By default, the very location where the expectation is created is captured, but the `traced` method can be use to add additional locations to the expectation.

```scala
object MySuite3 extends SimpleIOSuite {

  pureTest("And/Or composition") {
    foo
  }

  def foo(implicit loc : SourceLocation) = bar().traced(loc).traced(here)

  def bar() = baz().traced(here)

  def baz() = expect(1 != 1)

}
```
