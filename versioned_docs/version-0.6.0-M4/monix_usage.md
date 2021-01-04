---
id: version-0.6.0-M4-monix
title: Monix usage
original_id: monix
---

## Installation

You'll need to install an additional dependency in order to use weaver to test Monix programs.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-monix" % "0.6.0-M4" % Test
testFrameworks += new TestFramework("weaver.framework.Monix")
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-monix:0.6.0-M4"
  )
  def testFrameworks = Seq("weaver.framework.Monix")
}
```

## Usage

Testing Monix programs is practically the same as testing Cats Effect programs.

For basic usage, simply extend `SimpleTaskSuite`. Porting the example test, for instance:

```scala
import monix.eval.Task
import weaver.monixcompat._

object MySuite extends SimpleTaskSuite {

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

Monix program tests make use of shared resorces in the same way as Cats Effect program tests.

Extend `TaskSuite`, implementing the `sharedResource` member. For example:

```scala
import monix.eval.Task
import weaver.monixcompat._
import cats.effect._

// Using http4s
import org.http4s.client.blaze._
import org.http4s.client._

object HttpSuite extends TaskSuite {

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
