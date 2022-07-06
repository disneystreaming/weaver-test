---
id: multiple_suites_logging
title: Logging
---

Weaver only outputs the logs for tests that failed - the logs are neatly collected alongside the failure information and reported after all the tests have run.

Additionally, each log can have a context associated with it - which gets printed alongside the message.

```scala mdoc
import weaver._
import cats.effect._

object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  loggedTest("logging for success") { log =>
    for {
      x <- randomUUID
      y <- randomUUID
      _ <- log.info(s"Generated $x and $y")
    } yield expect(x != y)
  }

}

object MyAnotherSuite extends SimpleIOSuite {
  import scala.util.Random.alphanumeric

  val randomString = IO(alphanumeric.take(10).mkString(""))

  loggedTest("failure should print logs") { log =>
    for {
      currentTime <- IO.realTime.map(_.toSeconds)
      context = Map("time" -> currentTime.toString, "purpose" -> "docs")
      _ <- log.info("Starting the test...", context)
      x <- randomString
      _ <- log.debug(s"Generated random string: $x")
    } yield expect(x.length > 20)
  }
}
```

The report would look something like this:

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(MySuite, MyAnotherSuite))
```
