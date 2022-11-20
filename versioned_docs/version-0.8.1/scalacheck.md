---
id: version-0.8.1-scalacheck
title: ScalaCheck integration
original_id: scalacheck
---

Weaver comes with basic [ScalaCheck](https://www.scalacheck.org/) integration, allowing for property-based testing.

## Installation

You'll need to install an additional dependency in order to use ScalaCheck with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.8.1" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-scalacheck:0.8.1"
  )
}
```

## Usage

Add the `weaver.scalacheck.Checkers` mixin to use ScalaCheck within your test suite.

```scala
import org.scalacheck.Gen

import weaver._
import weaver.scalacheck._

object ForallExamples extends SimpleIOSuite with Checkers {

  // Using a single `Gen` instance
  test("Single Gen form") {
    // Takes a single, explicit `Gen` instance
    forall(Gen.posNum[Int]) { a =>
      expect(a > 0)
    }
  }

  // There is only one overload for the `forall` that takes an explicit `Gen` parameter
  // To use multiple `Gen` instances, compose them monadically before passing to `forall`
  test("Multiple Gen form") {
    // Compose into a single `Gen[(Int, Int)]`
    val gen = for {
      a <- Gen.posNum[Int]
      b <- Gen.posNum[Int]
    } yield (a, b)

    // Unapply the tuple to access individual members
    forall(gen) { case (a, b) =>
      expect(a > 0) and expect(b > 0)
    }
  }

  // Using a number of `Arbitrary` instances
  test("Arbitrary form") {
    // There are 6 overloads, to pass 1-6 parameters
    forall { (a1: Int, a2: Int, a3: Int) =>
      expect(a1 * a2 * a3 == a3 * a2 * a1)
    }
  }
  
  test("Failure example") {
    // There are 6 overloads, to pass 1-6 parameters
    forall { (a1: Int, a2: Int) =>
      expect(a1 + a2 % 2 == 0)
    }
  }

}
```

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>repl.MdocSessionMdocAppForallExamples</span>
<span style='color: green'>+&nbsp;</span>Single&nbsp;Gen&nbsp;form&nbsp;<span style='color: lightgray'><b>164ms</span></b>
<span style='color: green'>+&nbsp;</span>Multiple&nbsp;Gen&nbsp;form&nbsp;<span style='color: lightgray'><b>198ms</span></b>
<span style='color: green'>+&nbsp;</span>Arbitrary&nbsp;form&nbsp;<span style='color: lightgray'><b>182ms</span></b>
<span style='color: red'>-&nbsp;</span>Failure&nbsp;example&nbsp;<span style='color: lightgray'><b>56ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>repl.MdocSessionMdocAppForallExamples</span>
<span style='color: red'>-&nbsp;</span>Failure&nbsp;example&nbsp;<span style='color: lightgray'><b>56ms</span></b><br /><span style='color: red'>&nbsp;[0]&nbsp;Property&nbsp;test&nbsp;failed&nbsp;on&nbsp;try&nbsp;1&nbsp;with&nbsp;seed&nbsp;Seed.fromBase64("ZqsQvIxkp2YkG8mB0qWTyv2R0wz9HTLXNnFWIIcJO2M=")&nbsp;and&nbsp;input&nbsp;(654462009,1148905966)&nbsp;(modules/scalacheck/src/weaver/scalacheck/Checkers.scala:194)</span><br /><br /><span style='color: red'>&nbsp;[1]&nbsp;assertion&nbsp;failed&nbsp;(scalacheck.md:53)<br />&nbsp;[1]&nbsp;<br />&nbsp;[1]&nbsp;expect(a1&nbsp;+&nbsp;a2&nbsp;%&nbsp;2&nbsp;==&nbsp;0)<br />&nbsp;[1]&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;|&nbsp;&nbsp;|&nbsp;&nbsp;&nbsp;|<br />&nbsp;[1]&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;|&nbsp;&nbsp;0&nbsp;&nbsp;&nbsp;false<br />&nbsp;[1]&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;|&nbsp;1148905966<br />&nbsp;[1]&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;&nbsp;654462009<br />&nbsp;[1]&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;654462009</span>

Total&nbsp;4,&nbsp;Failed&nbsp;1,&nbsp;Passed&nbsp;3,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
