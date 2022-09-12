---
id: discipline
title: Discipline integration
---

Weaver comes with basic [Discipline](https://github.com/typelevel/discipline) integration, allowing property-based law testing.

## Installation

You'll need to install an additional dependency in order to use Discipline with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-discipline" % "@VERSION@" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-discipline:@VERSION@"
  )
}
```

## Usage

Add the `weaver.discipline.Discipline` mixin to a `FunSuite` to use Discipline within your test suite.

```scala mdoc:silent
import weaver._
import weaver.discipline._
import cats.kernel.laws.discipline.EqTests

object DisciplineTests extends FunSuite with Discipline {
  checkAll("Int", EqTests[Int].eqv)
  checkAll("Boolean", EqTests[Boolean].eqv)
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(DisciplineTests))
```
