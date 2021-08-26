---
id: version-0.6.5-zio
title: ZIO usage
original_id: zio
---

## Installation

You'll need to install an additional dependency in order to use weaver to test ZIO programs.

### SBT
```scala
libraryDependencies +=  "com.disneystreaming" %% "weaver-zio" % "0.6.5" % Test
testFrameworks += new TestFramework("weaver.framework.ZIO")
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"com.disneystreaming::weaver-zio:0.6.5"
  )
  def testFrameworks = Seq("weaver.framework.ZIO")
}
```

## Usage

Start with the following imports :

```scala
import weaver.ziocompat._
```

Weaver tries to respect ZIO's idiomatic usage, and leverages the [ZLayer](https://zio.dev/docs/howto/howto_use_layers) construct to provide environment. `ZLayer` can be trivially constructed from a `Managed` (the zio counterpart to `cats.effect.Resource`), and therefore encompasses `beforeAndAfterAll` semantics.

Assuming the following module :

```scala
import zio._
import org.http4s.client.blaze._
import org.http4s.client._

object modules {
  type Http = Has[Client[Task]]
  object Http {
    def get(uri : String) : RIO[modules.Http, Int] =
      ZIO.accessM(_.get.get(uri)(response => UIO(response.status.code)))
  }
}
import modules._
```

declare a shared layer as such :

```scala
// Needed to instantiate an http4s client against ZIO
import zio.interop.catz._

// ZIOSuite requires the type of the shared layer to be defined as a type
// parameter. That is because zio needs to implicitly derive some type-tag
// instance for its built-in dependency injection mechanism to work.
//
// NB, the default environment ZEnv is provided by default.
//
object HttpSuite extends ZIOSuite[Http] {

  // Sharing a single layer across all tests
  override val sharedLayer : ZLayer[ZEnv, Throwable, Http] =
    ZLayer.fromManaged {
      val makeHttpClient = ZIO.runtime[Any].map { implicit rts =>
        val exec = rts.platform.executor.asEC
        BlazeClientBuilder[Task](exec).resource.toManagedZIO
      }
      Managed.fromEffect(makeHttpClient).flatten
    }

  test("Standard test") {
    for {
      statusCode <- Http.get("https://httpbin.org/get")
    } yield expect(statusCode == 200)
  }

  test("A log module is available") {
    for {
      _          <- log.info("Calling https://httpbin.org/oops")
      statusCode <- Http.get("https://httpbin.org/oops")
    } yield expect(statusCode == 404)
  }

}
```

## Usage without shared layer

If you have no need for a shared layer, you can simply use the following.
Note that you can still use side effects provided by the default environment `ZEnv` 

```scala
import zio.duration._

object SimpleExample extends SimpleZIOSuite {

  pureTest("no side effect"){
    expect("hello".size == 5)
  }

  test("with side effects"){
    for {
      before <- zio.clock.currentDateTime
      _     <- zio.clock.sleep(2.second)
      after <- zio.clock.currentDateTime
    } yield expect(before != after)
  }

}
```


