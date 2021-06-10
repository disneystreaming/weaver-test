---
id: expectations
title: Expectations (assertions)
---

Expectations are pure, composable values. This forces developers to separate the test's checks from the scenario, which is generally cleaner/clearer.

The easiest way to construct expectactions is to call the `expect` macro, which is built using the [expecty](https://github.com/eed3si9n/expecty/) library.

```scala mdoc
import weaver._
import cats.effect.IO

object MySuite2 extends SimpleIOSuite {

  pureTest("And/Or composition (success)") {
    expect(1 != 2) and expect(2 != 1) or expect(2 != 3)
  }

  pureTest("And/Or composition (failure") {
    expect(1 != 2) and expect(2 == 1) or expect(2 != 3)
  }

  pureTest("Varargs composition (success)") {
    // expect(1 + 1 == 2) && expect (2 + 2 == 4) && expect(4 * 2 == 8)
    expect.all(1 + 1 == 2, 2 + 2 == 4, 4 * 2 == 8)
  }

  pureTest("Varargs composition (failure)") {
    // expect(1 + 1 == 2) && expect (2 + 2 == 4) && expect(4 * 2 == 8)
    expect.all(1 + 1 == 2, 2 + 2 == 5, 4 * 2 == 8)
  }

  pureTest("Pretty string diffs") {
    expect.same("foo", "bar")
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

```scala mdoc:invisible
import weaver.Expectations.Helpers._
```

## Expectations on collections (`Foldable`)

For anything that implements [`Foldable`](https://typelevel.org/cats/typeclasses/foldable.html) typeclass from cats,
two special methods are provided:

1. `forEach`
2. `exists`

```scala mdoc:silent
// OK
forEach(List(1, 2, 3))(i => expect(i < 5)) and
  forEach(Option("hello"))(msg => expect.same(msg, "hello")) and
  exists(List("a", "b", "c"))(i => expect(i == "c")) and
  exists(Vector(true, true, false))(i => expect(i == false))
```

```scala mdoc:fansi
// FAILURE
forEach(Vector("hello", "world"))(msg => expect.same(msg, "hello"))
```

```scala mdoc:fansi
// FAILURE
exists(Option(39))(i => expect(i > 50))
```

## Equality (`Eq`) comparison

1. Only uses equality as defined by `Eq[T]` typeclass
2. If values are different, the diff is rendered based on the string representation

   - If no `Show[T]` instance is found, the diff will fallback to `.toString` representation

```scala mdoc:silent
import cats.Eq
case class Test(d: Double)

implicit val eqTest: Eq[Test] = Eq.by[Test, Double](_.d)

// OK
expect.eql("hello", "hello") and
  expect.eql(List(1, 2, 3), List(1, 2, 3)) and
  expect.eql(Test(25.0), Test(25.0))
```

```scala mdoc:fansi
// FAILURE
expect.eql("hello", "world")
```

```scala mdoc:fansi
// FAILURE
expect.eql(List(1, 2, 3), List(1, 19, 3))
```

```scala mdoc:fansi
expect.eql(Test(25.0), Test(50.0))
```

### Relaxed equality comparison

`expect.same` is very similar to `expect.eql`, but if the `Eq[T]` instance is not found, it will
fall back to universal equality.

```scala mdoc
class Hello(d: Double) {
  override def toString = s"Hello to $d"
}
```

```scala mdoc:fansi
expect.same(new Hello(25.0), new Hello(50.0))
```

## Tracing locations of failed expectations

As of 0.5.0, failed expectations carry a `NonEmptyList[SourceLocation]`, which can be used to manually trace the callsites that lead to a failure.

By default, the very location where the expectation is created is captured, but the `traced` method can be use to add additional locations to the expectation.

```scala mdoc
object MySuite3 extends SimpleIOSuite {

  pureTest("And/Or composition") {
    foo
  }

  def foo(implicit loc : SourceLocation) = bar().traced(loc).traced(here)

  def bar() = baz().traced(here)

  def baz() = expect(1 != 1)

}
```
