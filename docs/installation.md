---
id: installation
title: Installation
---

Weaver-test is currently published for **Scala 2.12 and 2.13**

## SBT

Refer yourself to the [releases](https://github.bamtech.co/OSS/weaver-test/releases) page to know the latest released version, and add the following (or scoped equivalent) to your `build.sbt` file.

```scala
resolvers += "dss oss" at "https://artifactory.us-east-1.bamgrid.net/artifactory/oss-maven"

libraryDependencies += "com.disneystreaming.oss" %% "weaver-framework" % "x.y.z" % Test
testFrameworks += new TestFramework("weaver.framework.TestFramework")

// optionally (for ZIO usage)
libraryDependencies +=  "com.disneystreaming.oss" %% "weaver-zio" % "x.y.z" % Test

// optionally (for Scalacheck usage)
libraryDependencies +=  "com.disneystreaming.oss" %% "weaver-scalacheck" % "x.y.z" % Test
```

## Mill

```scala
import mill._, scalalib._

object foo extends ScalaModule {
  def scalaVersion = "2.13.1"

  object test extends Tests {
    def ivyDeps = Agg(
      ivy"com.disneystreaming.oss::weaver-framework:x.y.z",
      ivy"com.disneystreaming.oss::weaver-scalacheck:x.y.z",
      ivy"com.disneystreaming.oss::weaver-zio:x.y.z"
    )
    def testFrameworks = Seq("weaver.framework.TestFramework")
  }
}
```
