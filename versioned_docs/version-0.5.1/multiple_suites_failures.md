---
id: version-0.5.1-multiple_suites_failures
title: Failures
original_id: multiple_suites_failures
---

Weaver aggregates failures from all tests to output them after all the tests have finished

```scala
import weaver._
import cats.effect._

object MySuite extends SimpleIOSuite {
  val randomUUID = IO(java.util.UUID.randomUUID())

  simpleTest("failing test 1") {
    expect(1 >= 2)
  }
}

object MyAnotherSuite extends SimpleIOSuite {
  import scala.util.Random.alphanumeric

  val randomString = IO(alphanumeric.take(10).mkString(""))

  simpleTest("failing test 2") {
    for {
      x <- randomString
    } yield check(x).traced(here)
  }

  def check(x : String) = expect(x.length > 10)
}
```

The report looks like this:

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>MySuite</span>
<span style='color: red'>-&nbsp;</span>failing&nbsp;test&nbsp;1

<span style='color: cyan'>MyAnotherSuite</span>
<span style='color: red'>-&nbsp;</span>failing&nbsp;test&nbsp;2

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>MySuite</span>
<span style='color: red'>-&nbsp;</span>failing&nbsp;test&nbsp;1<br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(multiple_suites_failures.md:18)<br /><br />&nbsp;&nbsp;expect(1&nbsp;>=&nbsp;2)</span>

<span style='color: cyan'>MyAnotherSuite</span>
<span style='color: red'>-&nbsp;</span>failing&nbsp;test&nbsp;2<br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(multiple_suites_failures.md:34)<br />&nbsp;(multiple_suites_failures.md:31)<br /><br />&nbsp;&nbsp;def&nbsp;check(x&nbsp;:&nbsp;String)&nbsp;=&nbsp;expect(x.length&nbsp;>&nbsp;10)<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;10&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;false<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;mZkA16MDbl</span>

Total&nbsp;2,&nbsp;Failed&nbsp;2,&nbsp;Passed&nbsp;0,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
