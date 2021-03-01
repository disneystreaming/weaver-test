---
id: scalatest
title: scalatest integration
---

Weaver comes with [scalatest](http://scalatest.org/) matchers interop, allowing for matcher style testing.

## Installation

You'll need to install an additional dependency in order to use scalatest matchers with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalatest" % "@VERSION@" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-scalatest:@VERSION@"
  )
}
```

## Usage

Add the `weaver.scalatestcompat.IOShouldMatchers` or `weaver.scalatestcompat.IOMustMatchers` mixins to use scalatest matchers within your test suite.

```scala mdoc
import weaver.SimpleIOSuite

import weaver.scalatest.IOMustMatchers

object MustMatchersSpec extends SimpleIOSuite with IOMustMatchers {
  pureTest("pureTest { 1 mustBe 1 }") {
    1 mustBe 1
  }

  pureTest("pureTest { 1 must be (1) }") {
    1 must be (1)
  }


  pureTest("pureTest { 1 mustEqual 1 }") {
    1 mustEqual 1
  }

  pureTest("pureTest { 1 must equal (1) }") {
    1 must equal (1)
  }

  pureTest("pureTest { 1 must === (1) }") {
    1 must === (1)
  }
}
```
