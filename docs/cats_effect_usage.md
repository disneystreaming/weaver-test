---
id: cats
title: Cats-effect usage
---

## Installation

You'll need to install the following depending to test your programs against `cats.effect.IO`

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-cats" % "@VERSION@" % Test
testFrameworks += new TestFramework("weaver.framework.CatsEffect")
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-cats:@VERSION@"
  )
  def testFrameworks = Seq("weaver.framework.CatsEffect")
}
```

## Usage


Start with importing the following :

```scala mdoc
import weaver._
```

The most basic usage is to extend `SimpleIOSuite`. Tests are registered imperatively, very much like in scalatest's `FunSuite` or in `utest`, but their bodies are "weaved" together in a single `IO` that the framework executes when the build tool asks for it.


```scala mdoc
import cats.effect._

// Suites must be "objects" for them to be picked by the framework
object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  // A test for side-effecting functions
  simpleTest("hello side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect(x != y)
  }

}
```
