---
id: version-0.5.0-common_usage
title: Common usage
original_id: common_usage
---

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
  simpleTest("hello side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect(x != y)
  }

}
```

## Controlling parallelism

You can moderate the number of tests run in parallel by overriding the `maxParallelism` member of a suite.

If you wish to run tests serially, simply set it to 1.
