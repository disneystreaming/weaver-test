---
id: monix_bio
title: Monix BIO usage
---

Testing Monix BIO programs is very similar to testing regular Monix progams. 

Tests must return `monix.bio.Task[Expectation]` instances.

Note that in Monix BIO `Task[A]` [is an alias](https://bio.monix.io/docs/introduction) for the `IO[Throwable, A]` effect.

## Installation

You'll need to install an additional dependency in order to use weaver to test Monix BIO rograms.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-monix-bio" % "@VERSION@" % Test
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-monix-bio:@VERSION@"
  )
}
```

## Usage

For basic usage, simply extend `SimpleIOSuite`. Porting the example test, for instance:

```scala mdoc
import monix.bio.Task
import weaver.monixbiocompat._

object MySuite extends SimpleIOSuite {

  val randomUUID = Task(java.util.UUID.randomUUID())  // Use of `Task` instead of `IO`

  simpleTest("hello side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect(x != y)
  }

}
```

## Usage with shared resources

Monix BIO program tests make use of shared resorces in the same way as Cats Effect program tests.

Extend `IOSuite`, implementing the `sharedResource` member. For example:

```scala mdoc
import monix.bio.Task
import weaver.monixbiocompat._
import cats.effect._

// Using http4s
import org.http4s.client.blaze._
import org.http4s.client._

object HttpSuite extends IOSuite {

  // Sharing a single http client across all tests
  override type Res = Client[Task]
  override def sharedResource : Resource[Task, Res] =
    BlazeClientBuilder[Task](scheduler).resource

  // The test receives the shared client as an argument
  test("Good requests lead to good results") { httpClient =>
    for {
      statusCode <- httpClient.get("https://httpbin.org/get"){
        response => Task.pure(response.status.code)
      }
    } yield expect(statusCode == 200)
  }

  test("Bad requests lead to bad results") { httpClient =>
    for {
      statusCode <- httpClient.get("https://httpbin.org/oops"){
        response => Task.pure(response.status.code)
      }
    } yield expect(statusCode == 404)
  }


}
```
