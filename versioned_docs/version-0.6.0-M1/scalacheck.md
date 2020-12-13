---
id: version-0.6.0-M1-scalacheck
title: ScalaCheck integration
original_id: scalacheck
---

Weaver comes with basic [ScalaCheck](https://www.scalacheck.org/) integration, allowing for property-based testing.

## Installation

You'll need to install an additional dependency in order to use ScalaCheck with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.6.0-M1" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-scalacheck:0.6.0-M1"
  )
}
```

## Usage

Add the `weaver.scalacheck.IOCheckers` mixin to use ScalaCheck within your test suite.

```scala
import org.scalacheck.Gen

import weaver._
import weaver.scalacheck._

object ForallExamples extends SimpleIOSuite with IOCheckers {

  // Using a single `Gen` instance
  simpleTest("Single Gen form") {
    // Takes a single, explicit `Gen` instance
    forall(Gen.posNum[Int]) { a =>
      expect(a > 0)
    }
  }

  // There is only one overload for the `forall` that takes an explicit `Gen` parameter
  // To use multiple `Gen` instances, compose them monadically before passing to `forall`
  simpleTest("Multiple Gen form") {
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
  simpleTest("Arbitrary form") {
    // There are 6 overloads, to pass 1-6 parameters
    forall { (a1: Int, a2: Int, a3: Int) =>
      expect(a1 * a2 * a3 == a3 * a2 * a1)
    }
  }

}
```
