---
id: version-0.6.7-funsuite
title: Pure tests
original_id: funsuite
---

If your tests do not require any capability provided by effect-types, you can use a simplified interface `FunSuite`,
which comes with a single `test` method and does not allow effectful tests.

Tests in `FunSuite` are executed sequentially and without the performance overhead of effect
management.


```scala
object CatsFunSuite extends weaver.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }

  test("fails")   { expect(Some(25).contains(5)) }

  test("throws")  { throw new RuntimeException("oops") }
}
```

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>CatsFunSuite</span>
<span style='color: green'>+&nbsp;</span>asserts&nbsp;<span style='color: lightgray'><b>1ms</span></b>
<span style='color: red'>-&nbsp;</span>fails&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: red'>-&nbsp;</span>throws&nbsp;<span style='color: lightgray'><b>0ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>CatsFunSuite</span>
<span style='color: red'>-&nbsp;</span>fails&nbsp;<span style='color: lightgray'><b>2ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(funsuite.md:11)<br /><br />&nbsp;&nbsp;test("fails")&nbsp;&nbsp;&nbsp;{&nbsp;expect(Some(25).contains(5))&nbsp;}<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Some(25)&nbsp;false</span>
<span style='color: red'>-&nbsp;</span>throws&nbsp;<span style='color: lightgray'><b>0ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;RuntimeException:&nbsp;oops</span><br /><br /><span style='color: red'>&nbsp;&nbsp;funsuite.md:13&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;repl.MdocSession$App$CatsFunSuite$$anonfun$3#apply<br />&nbsp;&nbsp;funsuite.md:13&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;repl.MdocSession$App$CatsFunSuite$$anonfun$3#apply<br />&nbsp;&nbsp;Try.scala:210&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;scala.util.Try$#apply<br />&nbsp;&nbsp;Test.scala:32&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.Test$#pure<br />&nbsp;&nbsp;suites.scala:135&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#$anonfun$test$1<br />&nbsp;&nbsp;suites.scala:147&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#$anonfun$pureSpec$4<br />&nbsp;&nbsp;List.scala:250&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;scala.collection.immutable.List#map<br />&nbsp;&nbsp;List.scala:79&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;scala.collection.immutable.List#map<br />&nbsp;&nbsp;suites.scala:147&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#pureSpec<br />&nbsp;&nbsp;suites.scala:150&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#spec<br />&nbsp;&nbsp;Runner.scala:29&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.Runner#$anonfun$run$4<br />&nbsp;&nbsp;Stream.scala:1990&nbsp;&nbsp;&nbsp;&nbsp;fs2.Stream$#$anonfun$parEvalMap$3<br />&nbsp;&nbsp;<snipped>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.internals.<...><br />&nbsp;&nbsp;<snipped>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;java.util.concurrent.<...></span>

Total&nbsp;3,&nbsp;Failed&nbsp;2,&nbsp;Passed&nbsp;1,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>

A `FunSuite` alias is provided in each of the frameworks supported by weaver:

```scala
object MonixFunSuite extends weaver.monixcompat.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }
}

object MonixBIOFunSuite extends weaver.monixbiocompat.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }
}

object ZioBIOFunSuite extends weaver.ziocompat.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }
}
```
