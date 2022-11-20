---
id: version-0.8.1-funsuite
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
<span style='color: cyan'>repl.MdocSessionMdocAppCatsFunSuite</span>
<span style='color: green'>+&nbsp;</span>asserts&nbsp;<span style='color: lightgray'><b>1ms</span></b>
<span style='color: red'>-&nbsp;</span>fails&nbsp;<span style='color: lightgray'><b>1ms</span></b>
<span style='color: red'>-&nbsp;</span>throws&nbsp;<span style='color: lightgray'><b>0ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>repl.MdocSessionMdocAppCatsFunSuite</span>
<span style='color: red'>-&nbsp;</span>fails&nbsp;<span style='color: lightgray'><b>1ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(funsuite.md:11)<br /><br />&nbsp;&nbsp;test("fails")&nbsp;&nbsp;&nbsp;{&nbsp;expect(Some(25).contains(5))&nbsp;}<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Some(25)&nbsp;false</span>
<span style='color: red'>-&nbsp;</span>throws&nbsp;<span style='color: lightgray'><b>0ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;RuntimeException:&nbsp;oops</span><br /><br /><span style='color: red'>&nbsp;&nbsp;funsuite.md:13&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;repl.MdocSession$MdocApp$CatsFunSuite$$anonfun$3#apply<br />&nbsp;&nbsp;funsuite.md:13&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;repl.MdocSession$MdocApp$CatsFunSuite$$anonfun$3#apply<br />&nbsp;&nbsp;Try.scala:210&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;scala.util.Try$#apply<br />&nbsp;&nbsp;Test.scala:31&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.Test$#pure<br />&nbsp;&nbsp;suites.scala:187&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#$anonfun$test$1<br />&nbsp;&nbsp;suites.scala:197&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#$anonfun$pureSpec$1<br />&nbsp;&nbsp;List.scala:250&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;scala.collection.immutable.List#map<br />&nbsp;&nbsp;List.scala:79&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;scala.collection.immutable.List#map<br />&nbsp;&nbsp;suites.scala:197&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#pureSpec<br />&nbsp;&nbsp;suites.scala:201&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.FunSuiteF#spec<br />&nbsp;&nbsp;Runner.scala:33&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;weaver.Runner#$anonfun$run$5<br />&nbsp;&nbsp;Stream.scala:2140&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fs2.Stream#$anonfun$parEvalMapAction$7<br />&nbsp;&nbsp;ApplicativeError.scala:269&nbsp;&nbsp;&nbsp;&nbsp;cats.ApplicativeError#catchNonFatal<br />&nbsp;&nbsp;ApplicativeError.scala:268&nbsp;&nbsp;&nbsp;&nbsp;cats.ApplicativeError#catchNonFatal$<br />&nbsp;&nbsp;IO.scala:1605&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.IO$$anon$4#catchNonFatal<br />&nbsp;&nbsp;Stream.scala:2140&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;fs2.Stream#$anonfun$parEvalMapAction$6<br />&nbsp;&nbsp;IOFiber.scala:1169&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.IOFiber#succeeded<br />&nbsp;&nbsp;IOFiber.scala:269&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.IOFiber#runLoop<br />&nbsp;&nbsp;IOFiber.scala:1316&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.IOFiber#execR<br />&nbsp;&nbsp;IOFiber.scala:118&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.IOFiber#run<br />&nbsp;&nbsp;WorkerThread.scala:585&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;cats.effect.unsafe.WorkerThread#run</span>

Total&nbsp;3,&nbsp;Failed&nbsp;2,&nbsp;Passed&nbsp;1,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
