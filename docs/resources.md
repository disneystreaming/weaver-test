---
id: resources
title: Sharing resources
---

Resources can be shared across tests, this is done by implementing a method that returns [cats.effect.Resource](https://typelevel.org/cats-effect/datatypes/resource.html).

This is also how you would translate the traditional `beforeAll` and `afterAll` methods, as the resources are acquired before tests are run, and cleaned-up after they finish.


```scala mdoc
import weaver._
import cats.effect._

// Using http4s
import org.http4s.client.blaze._
import org.http4s.client._
import scala.concurrent.ExecutionContext.global

object HttpSuite extends IOSuite {

  // Sharing a single http client across all tests
  override type Res = Client[IO]
  override def sharedResource : Resource[IO, Res] =
    BlazeClientBuilder[IO](global).resource

  // The test receives the shared client as an argument
  test("Good requests lead to good results") { httpClient =>
    for {
      statusCode <- httpClient.get("https://httpbin.org/get"){
        response => IO.pure(response.status.code)
      }
    } yield expect(statusCode == 200)
  }

  test("Bad requests lead to bad results") { httpClient =>
    for {
      statusCode <- httpClient.get("https://httpbin.org/oops"){
        response => IO.pure(response.status.code)
      }
    } yield expect(statusCode == 404)
  }


}
```

### Resources lifecycle

We can demonstrate the resource lifecycle with this example:

```scala mdoc
import java.util.concurrent.ConcurrentLinkedQueue

// We will store the messages in this queue
val order = new ConcurrentLinkedQueue[String]()

object ResourceDemo extends IOSuite {

  def record(msg: String) = IO(order.add(msg)).void

  override type Res = Int
  override def sharedResource = {
    val acquire = record("Acquiring resource") *> IO.pure(42)
    val release = (i: Int) => record(s"Releasing resource $i")
    Resource.make(acquire)(release)
  }

  test("Test 1") { res =>
    record(s"Test 1 is using resource $res").as(success)
  }

  test("Test 2") { res => 
    record(s"Test 2 is using resource $res").as(expect(res == 45))
  }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(ResourceDemo).unsafeRunSync())

println("Contents of `order` are:\n")
println("```")
order.toArray.foreach { el => 
  println(s"// * $el")
}
println("```")
```
