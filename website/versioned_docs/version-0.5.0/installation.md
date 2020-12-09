---
id: version-0.5.0-installation
title: Installation
original_id: installation
---

Weaver-test is currently published for **Scala 2.12 and 2.13**

## SBT

Refer yourself to the [releases](https://github.com/disneystreaming/weaver-test/releases) page to know the latest released version, and add the following (or scoped equivalent) to your `build.sbt` file.

```scala
libraryDependencies += "com.disneystreaming" %% "weaver-framework" % "0.5.0" % Test
testFrameworks += new TestFramework("weaver.framework.TestFramework")

// optionally (for ZIO usage)
libraryDependencies +=  "com.disneystreaming" %% "weaver-zio" % "0.5.0" % Test

// optionally (for Scalacheck usage)
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "0.5.0" % Test
```

## Mill

```scala
import mill._, scalalib._

object foo extends ScalaModule {
  def scalaVersion = "2.13.1"

  object test extends Tests {
    def ivyDeps = Agg(
      ivy"com.disneystreaming::weaver-framework:0.5.0",
      ivy"com.disneystreaming::weaver-scalacheck:0.5.0",
      ivy"com.disneystreaming::weaver-zio:0.5.0"
    )
    def testFrameworks = Seq("weaver.framework.TestFramework")
  }
}
```
