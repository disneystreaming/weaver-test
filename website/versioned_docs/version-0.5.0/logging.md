---
id: version-0.5.0-logging
title: Logging information
original_id: logging
---

Weaver provides each individual test with a lazy-logger. The log statements only get reported if the test is unsuccessful. Because tests in weaver run in parallel by default, this makes it easier to tie printed information to the test it originated from.

```scala
import weaver._
import cats.effect._

object LoggedTests extends IOSuite {

  // Only the logger is received as an argument
  loggedTest("Just logging some stuff") { log =>
    for {
      _ <- log.info("oopsie daisy")
    } yield expect(2 + 2 == 5)
  }


  // We can oviously have tests receive loggers AND shared resources
  override type Res = String
  override def sharedResource : Resource[IO, Res] =
    Resource.pure[IO, Res]("hello")

  // Both the logger and the resource are received as arguments
  test("Good requests lead to good results") { (sharedString, log) =>
    for {
      _ <- log.info(sharedString)
    } yield expect(2 + 2 == 4)
  }

}
```

