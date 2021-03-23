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

Add the `weaver.scalacheck.IOCheckers` mixin to use ScalaCheck within your test suite.

```scala mdoc:silent
import weaver._
import weaver.discipline._
import cats.kernel.laws.discipline.EqTests

object DisciplineTests extends SimpleIOSuite with Discipline {
  
  override def maxParallelism = 1

  checkAll("Int", EqTests[Int].eqv)
  checkAll("Boolean", EqTests[Int].eqv)
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(DisciplineTests).unsafeRunSync())
```
