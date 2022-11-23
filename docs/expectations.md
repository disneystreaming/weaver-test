---
id: expectations
title: Expectations (assertions)
---

Expectations are pure, composable values. This forces developers to separate the test's checks from the scenario, which is generally cleaner/clearer.

The easiest way to construct expectactions is to call the `expect` macro, which is built using the [expecty](https://github.com/eed3si9n/expecty/) library.

## TL;DR

- Assert on boolean values using `expect`: 
   
   ```scala mdoc:compile-only
   expect(myVar == 25 && list.size == 4)
   ```

- Compose expectations using `and`/`or`
  
  ```scala mdoc:compile-only
  (expect(1 == 1) and expect(2 > 1)) or expect(5 == 5)
  ```

- ... or their symbolic aliases `&&`/`||`

  ```scala mdoc:compile-only
  (expect(1 == 1) && expect(2 > 1)) || expect(5 == 5)
  ```

- Compose expectations using `xor`

  ```scala mdoc:compile-only
  expect(1 == 1) xor expect(2 == 2)
  ```

- Use varargs short form for asserting on all boolean values

  ```scala mdoc:compile-only
  expect.all(1 == 1, 2 == 2, 3 > 2)
  ```

- Use `forEach` to test every element of a collection (or anything that
    implements `Foldable`)

  ```scala mdoc:compile-only
  forEach(List(1, 2, 3))(i => expect(i < 5))
  ```

- Use `exists` to assert that at least one element of collection matches
    expectations:

  ```scala mdoc:compile-only
  exists(Option(5))(n => expect(n > 3)
  ```

- Use `expect.eql` for strict equality comparison (types that implement `Eq`
    typeclass) and string representation diffing (using `Show` typeclass, fall
    back to `toString` if no instance found) in
    case of failure

  ```scala mdoc:compile-only
  expect.eql(List(1, 2, 3), (1 to 3).toList)
  ```

  See below how the output looks in case of failure

- Use `expect.same` for relaxed equality comparison (if no `Eq` instance is
    found, fall back to universal equality) and relaxed string diffing (fall
    back to `toString` implementation)

  ```scala mdoc:compile-only
  expect.same(List(1, 2, 3), (1 to 3).toList)
  ```

- Use `success` or `failure` to create succeeding/failing expectations without
    conditions

  ```scala mdoc:compile-only
  val result = if(5 == 5) success else failure("oh no")
  ```

- Use `.failFast` to evaluate the expectation eagerly and raise the assertion error in your effect type

  ```scala mdoc:compile-only
  for {
    x <- IO("hello")
    _ <- expect(x.length == 4).failFast
    y = x + "bla"
    _ <- expect(y.size > x.size).failFast
  } yield expect(y.contains(x))
  ```

## Example suite 

```scala mdoc
import weaver._
import cats.effect.IO

object ExpectationsSuite extends SimpleIOSuite {

  object A {
    object B {
      object C {
        def test(a: Int) = a + 5
      }
    }
  }

  pureTest("Simple expectations (success)") {
    val z = 15
    
    expect(A.B.C.test(z) == z + 5)
  }
  
  pureTest("Simple expectations (failure)") {
    val z = 15
    
    expect(A.B.C.test(z) % 7 == 0)
  }


  pureTest("And/Or composition (success)") {
    expect(1 != 2) and expect(2 != 1) or expect(2 != 3)
  }

  pureTest("And/Or composition (failure") {
    (expect(1 != 2) and expect(2 == 1)) or expect(2 == 3)
  }

  pureTest("Varargs composition (success)") {
    // expect(1 + 1 == 2) && expect (2 + 2 == 4) && expect(4 * 2 == 8)
    expect.all(1 + 1 == 2, 2 + 2 == 4, 4 * 2 == 8)
  }

  pureTest("Varargs composition (failure)") {
    // expect(1 + 1 == 2) && expect (2 + 2 == 4) && expect(4 * 2 == 8)
    expect.all(1 + 1 == 2, 2 + 2 == 5, 4 * 2 == 8)
  }

  pureTest("Working with collections (success)") {
    forEach(List(1, 2, 3))(i => expect(i < 5)) and
      forEach(Option("hello"))(msg => expect.same(msg, "hello")) and
      exists(List("a", "b", "c"))(i => expect(i == "c")) and
      exists(Vector(true, true, false))(i => expect(i == false))
  }

  pureTest("Working with collections (failure 1)") {
    forEach(Vector("hello", "world"))(msg => expect.same(msg, "hello"))
  }

  pureTest("Working with collections (failure 2)") {
    exists(Option(39))(i => expect(i > 50))
  }

  import cats.Eq
  case class Test(d: Double)

  implicit val eqTest: Eq[Test] = Eq.by[Test, Double](_.d)

  pureTest("Strict equality (success)") {
    expect.eql("hello", "hello") and
      expect.eql(List(1, 2, 3), List(1, 2, 3)) and
      expect.eql(Test(25.0), Test(25.0))  
  }

  pureTest("Strict equality (failure 1)") {
    expect.eql("hello", "world")
  }

  pureTest("Strict equality (failure 2)") {
    expect.eql(List(1, 2, 3), List(1, 19, 3))
  }

  pureTest("Strict equality (failure 3)") {
    expect.eql(Test(25.0), Test(50.0))
  }

  // Note that we don't have an instance of Eq[Hello]
  // anywhere in scope
  class Hello(val d: Double) {
    override def toString = s"Hello to $d"

    override def equals(other: Any) = 
      if(other != null && other.isInstanceOf[Hello])
        other.asInstanceOf[Hello].d == this.d
      else 
        false
  }

  pureTest("Relaxed equality comparison (success)") {
    expect.same(new Hello(25.0), new Hello(25.0))
  }
  
  pureTest("Relaxed equality comparison (failure)") {
    expect.same(new Hello(25.0), new Hello(50.0))
  }

  pureTest("Non macro-based expectations") {
    val condition : Boolean = false
    if (condition) success else failure("Condition failed")
  }

  test("Failing fast expectations") {
    for {
      h <- IO.pure("hello")
      _ <- expect(h.isEmpty).failFast
    } yield success
  }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ExpectationsSuite))
```

## Tracing locations of failed expectations

As of 0.5.0, failed expectations carry a `NonEmptyList[SourceLocation]`, which can be used to manually trace the callsites that lead to a failure.

By default, the very location where the expectation is created is captured, but the `traced` method can be use to add additional locations to the expectation.

```scala mdoc
object TracingSuite extends SimpleIOSuite {

  pureTest("Tracing example") {
    foo
  }

  def foo(implicit loc : SourceLocation) = bar().traced(loc).traced(here)

  def bar() = baz().traced(here)

  def baz() = expect(1 != 1)
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(TracingSuite))
```
