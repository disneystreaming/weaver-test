---
id: specs2
title: specs2 integration
---

Weaver comes with [specs2](http://specs2.org/) matchers interop, allowing for matcher style testing.

## Installation

You'll need to install an additional dependency in order to use specs2 matchers with Weaver.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-specs2" % "@VERSION@" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-specs2:@VERSION@"
  )
}
```

## Usage

Add the `weaver.specs2compat.IOMatchers` mixin to use specs2 matchers within your test suite.

```scala mdoc
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
}

```
