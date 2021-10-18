---
id: version-0.6.7-discipline
title: Discipline integration
original_id: discipline
---

Weaver comes with basic [Discipline](https://github.com/typelevel/discipline) integration, allowing property-based law testing.

## Installation

You'll need to install an additional dependency in order to use Discipline with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-discipline" % "0.6.7" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-discipline:0.6.7"
  )
}
```

## Usage

Add the `weaver.discipline.Discipline` mixin to a `FunSuite` to use Discipline within your test suite.

```scala
import weaver._
import weaver.discipline._
import cats.kernel.laws.discipline.EqTests

object DisciplineTests extends FunSuite with Discipline {
  checkAll("Int", EqTests[Int].eqv)
  checkAll("Boolean", EqTests[Boolean].eqv)
}
```

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>DisciplineTests</span>
<span style='color: green'>+&nbsp;</span>Int:&nbsp;eq.antisymmetry&nbsp;eq&nbsp;<span style='color: lightgray'><b>60ms</span></b>
<span style='color: green'>+&nbsp;</span>Int:&nbsp;eq.reflexivity&nbsp;eq&nbsp;<span style='color: lightgray'><b>9ms</span></b>
<span style='color: green'>+&nbsp;</span>Int:&nbsp;eq.symmetry&nbsp;eq&nbsp;<span style='color: lightgray'><b>8ms</span></b>
<span style='color: green'>+&nbsp;</span>Int:&nbsp;eq.transitivity&nbsp;eq&nbsp;<span style='color: lightgray'><b>7ms</span></b>
<span style='color: green'>+&nbsp;</span>Boolean:&nbsp;eq.antisymmetry&nbsp;eq&nbsp;<span style='color: lightgray'><b>8ms</span></b>
<span style='color: green'>+&nbsp;</span>Boolean:&nbsp;eq.reflexivity&nbsp;eq&nbsp;<span style='color: lightgray'><b>2ms</span></b>
<span style='color: green'>+&nbsp;</span>Boolean:&nbsp;eq.symmetry&nbsp;eq&nbsp;<span style='color: lightgray'><b>3ms</span></b>
<span style='color: green'>+&nbsp;</span>Boolean:&nbsp;eq.transitivity&nbsp;eq&nbsp;<span style='color: lightgray'><b>1ms</span></b>

Total&nbsp;8,&nbsp;Failed&nbsp;0,&nbsp;Passed&nbsp;8,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
