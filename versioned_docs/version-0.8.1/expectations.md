---
id: version-0.8.1-expectations
title: Expectations (assertions)
original_id: expectations
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

```scala
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

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>repl.MdocSessionMdocAppExpectationsSuite</span>
<span style='color: green'>+&nbsp;</span>Simple&nbsp;expectations&nbsp;(success)&nbsp;<span style='color: lightgray'><b>45ms</span></b>
<span style='color: green'>+&nbsp;</span>And/Or&nbsp;composition&nbsp;(success)&nbsp;<span style='color: lightgray'><b>14ms</span></b>
<span style='color: green'>+&nbsp;</span>Varargs&nbsp;composition&nbsp;(success)&nbsp;<span style='color: lightgray'><b>1ms</span></b>
<span style='color: green'>+&nbsp;</span>Working&nbsp;with&nbsp;collections&nbsp;(success)&nbsp;<span style='color: lightgray'><b>21ms</span></b>
<span style='color: green'>+&nbsp;</span>Strict&nbsp;equality&nbsp;(success)&nbsp;<span style='color: lightgray'><b>4ms</span></b>
<span style='color: green'>+&nbsp;</span>Relaxed&nbsp;equality&nbsp;comparison&nbsp;(success)&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: red'>-&nbsp;</span>Simple&nbsp;expectations&nbsp;(failure)&nbsp;<span style='color: lightgray'><b>52ms</span></b>
<span style='color: red'>-&nbsp;</span>And/Or&nbsp;composition&nbsp;(failure&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: red'>-&nbsp;</span>Varargs&nbsp;composition&nbsp;(failure)&nbsp;<span style='color: lightgray'><b>0ms</span></b>
<span style='color: red'>-&nbsp;</span>Working&nbsp;with&nbsp;collections&nbsp;(failure&nbsp;1)&nbsp;<span style='color: lightgray'><b>13ms</span></b>
<span style='color: red'>-&nbsp;</span>Working&nbsp;with&nbsp;collections&nbsp;(failure&nbsp;2)&nbsp;<span style='color: lightgray'><b>3ms</span></b>
<span style='color: red'>-&nbsp;</span>Strict&nbsp;equality&nbsp;(failure&nbsp;1)&nbsp;<span style='color: lightgray'><b>9ms</span></b>
<span style='color: red'>-&nbsp;</span>Strict&nbsp;equality&nbsp;(failure&nbsp;2)&nbsp;<span style='color: lightgray'><b>10ms</span></b>
<span style='color: red'>-&nbsp;</span>Strict&nbsp;equality&nbsp;(failure&nbsp;3)&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: red'>-&nbsp;</span>Relaxed&nbsp;equality&nbsp;comparison&nbsp;(failure)&nbsp;<span style='color: lightgray'><b>3ms</span></b>
<span style='color: red'>-&nbsp;</span>Non&nbsp;macro-based&nbsp;expectations&nbsp;<span style='color: lightgray'><b>0ms</span></b>
<span style='color: red'>-&nbsp;</span>Failing&nbsp;fast&nbsp;expectations&nbsp;<span style='color: lightgray'><b>9ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>repl.MdocSessionMdocAppExpectationsSuite</span>
<span style='color: red'>-&nbsp;</span>Simple&nbsp;expectations&nbsp;(failure)&nbsp;<span style='color: lightgray'><b>52ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:33)<br /><br />&nbsp;&nbsp;expect(A.B.C.test(z)&nbsp;%&nbsp;7&nbsp;==&nbsp;0)<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;20&nbsp;&nbsp;&nbsp;15&nbsp;6&nbsp;&nbsp;&nbsp;false</span>
<span style='color: red'>-&nbsp;</span>And/Or&nbsp;composition&nbsp;(failure&nbsp;<span style='color: lightgray'><b>2ms</span></b><br /><span style='color: red'>&nbsp;[0]&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:42)<br />&nbsp;[0]&nbsp;<br />&nbsp;[0]&nbsp;(expect(1&nbsp;!=&nbsp;2)&nbsp;and&nbsp;expect(2&nbsp;==&nbsp;1))&nbsp;or&nbsp;expect(2&nbsp;==&nbsp;3)</span><br /><br /><span style='color: red'>&nbsp;[1]&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:42)<br />&nbsp;[1]&nbsp;<br />&nbsp;[1]&nbsp;(expect(1&nbsp;!=&nbsp;2)&nbsp;and&nbsp;expect(2&nbsp;==&nbsp;1))&nbsp;or&nbsp;expect(2&nbsp;==&nbsp;3)</span>
<span style='color: red'>-&nbsp;</span>Varargs&nbsp;composition&nbsp;(failure)&nbsp;<span style='color: lightgray'><b>0ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:52)<br /><br />&nbsp;&nbsp;expect.all(1&nbsp;+&nbsp;1&nbsp;==&nbsp;2,&nbsp;2&nbsp;+&nbsp;2&nbsp;==&nbsp;5,&nbsp;4&nbsp;*&nbsp;2&nbsp;==&nbsp;8)</span>
<span style='color: red'>-&nbsp;</span>Working&nbsp;with&nbsp;collections&nbsp;(failure&nbsp;1)&nbsp;<span style='color: lightgray'><b>13ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;Values&nbsp;not&nbsp;equal:&nbsp;(expectations.md:63)<br /><br />&nbsp;&nbsp;</span><span style='color: green'><b>[world]</span></b>&nbsp;&nbsp;|&nbsp;&nbsp;<span style='color: red'><b>[hello]</span></b>
<span style='color: red'>-&nbsp;</span>Working&nbsp;with&nbsp;collections&nbsp;(failure&nbsp;2)&nbsp;<span style='color: lightgray'><b>3ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:67)<br /><br />&nbsp;&nbsp;exists(Option(39))(i&nbsp;=>&nbsp;expect(i&nbsp;>&nbsp;50))<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;false<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;39</span>
<span style='color: red'>-&nbsp;</span>Strict&nbsp;equality&nbsp;(failure&nbsp;1)&nbsp;<span style='color: lightgray'><b>9ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;Values&nbsp;not&nbsp;equal:&nbsp;(expectations.md:82)<br /><br />&nbsp;&nbsp;</span><span style='color: green'><b>[hello]</span></b>&nbsp;&nbsp;|&nbsp;&nbsp;<span style='color: red'><b>[world]</span></b>
<span style='color: red'>-&nbsp;</span>Strict&nbsp;equality&nbsp;(failure&nbsp;2)&nbsp;<span style='color: lightgray'><b>10ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;Values&nbsp;not&nbsp;equal:&nbsp;(expectations.md:86)<br /><br />&nbsp;&nbsp;</span>List(1,&nbsp;<span style='color: green'><b>[2]</span></b>,&nbsp;3)&nbsp;&nbsp;|&nbsp;&nbsp;List(1,&nbsp;<span style='color: red'><b>[19]</span></b>,&nbsp;3)
<span style='color: red'>-&nbsp;</span>Strict&nbsp;equality&nbsp;(failure&nbsp;3)&nbsp;<span style='color: lightgray'><b>2ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;Values&nbsp;not&nbsp;equal:&nbsp;(expectations.md:90)<br /><br />&nbsp;&nbsp;</span>Test(<span style='color: green'><b>[25]</span></b>.0)&nbsp;&nbsp;|&nbsp;&nbsp;Test(<span style='color: red'><b>[50]</span></b>.0)
<span style='color: red'>-&nbsp;</span>Relaxed&nbsp;equality&nbsp;comparison&nbsp;(failure)&nbsp;<span style='color: lightgray'><b>3ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;Values&nbsp;not&nbsp;equal:&nbsp;(expectations.md:110)<br /><br />&nbsp;&nbsp;</span>Hello&nbsp;to&nbsp;<span style='color: green'><b>[25]</span></b>.0&nbsp;&nbsp;|&nbsp;&nbsp;Hello&nbsp;to&nbsp;<span style='color: red'><b>[50]</span></b>.0
<span style='color: red'>-&nbsp;</span>Non&nbsp;macro-based&nbsp;expectations&nbsp;<span style='color: lightgray'><b>0ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;Condition&nbsp;failed&nbsp;(expectations.md:115)</span>
<span style='color: red'>-&nbsp;</span>Failing&nbsp;fast&nbsp;expectations&nbsp;<span style='color: lightgray'><b>9ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:121)<br /><br />&nbsp;&nbsp;_&nbsp;<-&nbsp;expect(h.isEmpty).failFast<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;false<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;hello</span>

Total&nbsp;17,&nbsp;Failed&nbsp;11,&nbsp;Passed&nbsp;6,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>

## Tracing locations of failed expectations

As of 0.5.0, failed expectations carry a `NonEmptyList[SourceLocation]`, which can be used to manually trace the callsites that lead to a failure.

By default, the very location where the expectation is created is captured, but the `traced` method can be use to add additional locations to the expectation.

```scala
object TracingSuite extends SimpleIOSuite {

  pureTest("Tracing example") {
    foo
  }

  def foo(implicit loc : SourceLocation) = bar().traced(loc).traced(here)

  def bar() = baz().traced(here)

  def baz() = expect(1 != 1)
}
```

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>repl.MdocSessionMdocAppTracingSuite</span>
<span style='color: red'>-&nbsp;</span>Tracing&nbsp;example&nbsp;<span style='color: lightgray'><b>2ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>repl.MdocSessionMdocAppTracingSuite</span>
<span style='color: red'>-&nbsp;</span>Tracing&nbsp;example&nbsp;<span style='color: lightgray'><b>2ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(expectations.md:146)<br />&nbsp;(expectations.md:144)<br />&nbsp;(expectations.md:139)<br />&nbsp;(expectations.md:142)<br /><br />&nbsp;&nbsp;def&nbsp;baz()&nbsp;=&nbsp;expect(1&nbsp;!=&nbsp;1)</span>

Total&nbsp;1,&nbsp;Failed&nbsp;1,&nbsp;Passed&nbsp;0,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
