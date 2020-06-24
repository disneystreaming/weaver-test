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

object HttpSuite extends IOSuite {

  // Sharing a single http client across all tests
  override type Res = Client[IO]
  override def sharedResource : Resource[IO, Res] =
    BlazeClientBuilder[IO](ec).resource

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

