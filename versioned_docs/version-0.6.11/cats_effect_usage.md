---
id: version-0.6.11-cats
title: Cats-effect usage
original_id: cats
---

## Installation

You'll need to install the following depending to test your programs against `cats.effect.IO`

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-cats" % "0.6.11" % Test
testFrameworks += new TestFramework("weaver.framework.CatsEffect")
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-cats:0.6.11"
  )
  def testFrameworks = Seq("weaver.framework.CatsEffect")
}
```

## Usage


Start with importing the following :

```scala
import weaver._
```

The most basic usage is to extend `SimpleIOSuite`. Tests are registered imperatively, very much like in scalatest's `FunSuite` or in `utest`, but their bodies are "weaved" together in a single `IO` that the framework executes when the build tool asks for it.


```scala
import cats.effect._

// Suites must be "objects" for them to be picked by the framework
object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  // A test for side-effecting functions
  test("hello side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect(x != y)
  }

}
```
