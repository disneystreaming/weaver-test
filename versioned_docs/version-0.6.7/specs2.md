---
id: version-0.6.7-specs2
title: specs2 integration
original_id: specs2
---

Weaver comes with [specs2](http://specs2.org/) matchers interop, allowing for matcher style testing.

## Installation

You'll need to install an additional dependency in order to use specs2 matchers with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-specs2" % "0.6.7" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-specs2:0.6.7"
  )
}
```

## Usage

Add the `weaver.specs2compat.IOMatchers` mixin to use specs2 matchers within your test suite.

```scala
import weaver.SimpleIOSuite

import weaver.specs2compat.IOMatchers

object MatchersSpec extends SimpleIOSuite with IOMatchers {
  pureTest("pureTest { 1 must beEqualTo(1) }") {
    1 must beEqualTo(1)
  }

  pureTest("pureTest { 1 must be_==(1) }") {
    1 must be_==(1)
  }

  pureTest("pureTest { 1 mustEqual 1 }") {
    1 mustEqual 1
  }

  pureTest("pureTest { 1 === 1 }") {
    1 === 1
  }

  pureTest("pureTest { 1 must beEqualTo(1) }") {
    1 must beEqualTo(1)
  }

  pureTest("pureTest { 1 must be_==(1) }") {
    1 must be_==(1)
  }

  pureTest("pureTest { 1 mustEqual 1 }") {
    1 mustEqual 1
  }

  pureTest("pureTest { 1 === 1 }") {
    1 === 1
  }
  
  pureTest("failure example") {
    1 must beEqualTo(2)
  }
}
```

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>MatchersSpec</span>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;must&nbsp;beEqualTo(1)&nbsp;}&nbsp;<span style='color: lightgray'><b>32ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;must&nbsp;be_==(1)&nbsp;}&nbsp;<span style='color: lightgray'><b>26ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;mustEqual&nbsp;1&nbsp;}&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;===&nbsp;1&nbsp;}&nbsp;<span style='color: lightgray'><b>13ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;must&nbsp;beEqualTo(1)&nbsp;}&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;must&nbsp;be_==(1)&nbsp;}&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;mustEqual&nbsp;1&nbsp;}&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: green'>+&nbsp;</span>pureTest&nbsp;{&nbsp;1&nbsp;===&nbsp;1&nbsp;}&nbsp;<span style='color: lightgray'><b>1ms</span></b>
<span style='color: red'>-&nbsp;</span>failure&nbsp;example&nbsp;<span style='color: lightgray'><b>6ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>MatchersSpec</span>
<span style='color: red'>-&nbsp;</span>failure&nbsp;example&nbsp;<span style='color: lightgray'><b>6ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;1&nbsp;!=&nbsp;2&nbsp;(specs2.md:48)</span>

Total&nbsp;9,&nbsp;Failed&nbsp;1,&nbsp;Passed&nbsp;8,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
