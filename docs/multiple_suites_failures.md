---
id: multiple_suites_failures
title: Failures
---

Weaver aggregates failures from all tests to output them after all the tests have finished

```scala mdoc
import weaver._
import cats.effect._

object MySuite extends SimpleIOSuite {
  val randomUUID = IO(java.util.UUID.randomUUID())

  pureTest("failing test 1") {
    expect(1 >= 2)
  }
}

object MyAnotherSuite extends SimpleIOSuite {
  import scala.util.Random.alphanumeric

  val randomString = IO(alphanumeric.take(10).mkString(""))

  test("failing test 2") {
    for {
      x <- randomString
    } yield check(x).traced(here)
  }

  def check(x : String) = expect(x.length > 10)
}
```

The report looks like this:

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(MySuite, MyAnotherSuite))
```
