---
id: installation
title: Installation
---

Weaver-test is currently published for **Scala 2.12 and 2.13**

## SBT

Refer yourself to the [releases](https://github.com/disneystreaming/weaver-test/releases) page to know the latest released version, and add the following (or scoped equivalent) to your `build.sbt` file.

```scala
libraryDependencies += "com.disneystreaming" %% "weaver-framework" % "@VERSION@" % Test
testFrameworks += new TestFramework("weaver.framework.TestFramework")

// optionally (for ZIO usage)
libraryDependencies +=  "com.disneystreaming" %% "weaver-zio" % "@VERSION@" % Test

// optionally (for Scalacheck usage)
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "@VERSION@" % Test
```

## Mill

```scala
import mill._, scalalib._

object foo extends ScalaModule {
  def scalaVersion = "2.13.1"

  object test extends Tests {
    def ivyDeps = Agg(
      ivy"com.disneystreaming::weaver-framework:@VERSION@",
      ivy"com.disneystreaming::weaver-scalacheck:@VERSION@",
      ivy"com.disneystreaming::weaver-zio:@VERSION@"
    )
    def testFrameworks = Seq("weaver.framework.TestFramework")
  }
}
```
