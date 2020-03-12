[![Build Status](https://oss-drone.bamgrid.com/api/badges/OSS/weaver-test/status.svg)](https://oss-drone.bamgrid.com/OSS/weaver-test)

# Weaver-test

A test-framework built on [cats-effect](https://github.com/typelevel/cats-effect) and
[fs2](https://github.com/functional-streams-for-scala/fs2), with [zio](https://zio.dev) interop.

## Installation

Weaver-test is currently published for **Scala 2.12 and 2.13**

### SBT

Refer yourself to the [releases](https://github.bamtech.co/OSS/weaver-test/releases) page to know the latest released version, and add the following (or scoped equivalent) to your `build.sbt` file.

```scala
resolvers += "dss oss" at "https://artifactory.us-east-1.bamgrid.net/artifactory/oss-maven"

libraryDependencies += "com.disneystreaming.oss" %% "weaver-framework" % "x.y.z" % Test
testFrameworks += new TestFramework("weaver.framework.TestFramework")

// optionally (for ZIO usage)
libraryDependencies +=  "com.disneystreaming.oss" %% "weaver-zio" % "x.y.z" % Test

// optionally (for Scalacheck usage)
libraryDependencies +=  "com.disneystreaming.oss" %% "weaver-scalacheck" % "x.y.z" % Test

```

## Motivation

![time](docs/assets/time.png)

Weaver aims at providing a nice experience when writing and running tests :

* tests within a suite are run in parallel for quickest results possible
* expectations (ie assertions) are composable values. This forces
developers to separate the scenario of the test from the checks they perform,
which generally keeps tests cleaner / clearer.
* failures are aggregated and reported at the end of the run. This prevents the developer from having to "scroll up" forever when trying to understand what failed.
* a lazy logger is provided for each test, and log statements are only displayed in case of a test failure. This lets the developer enrich their tests with clues and works perfectly well with parallel runs

## API

### Suites

#### SimpleIOSuite

The suite that is most familiar to developers :

```scala
import weaver.SimpleIOSuite
import cats.effect._

// Suites must be "objects" for them to be picked by the framework
object MySuite extends SimpleIOSuite {

  // A test for non-effectful (pure) functions
  pureTest("hello pure"){
    expect("hello".size == 6)
  }

  val random = IO(java.util.UUID.randomUUID())

  // A test for side-effecting functions
  simpleTest("hello side-effects") {
    for {
      x <- random
      y <- random
    } yield expect(x != y)
  }

  // A test with logs
  loggedTest("hello logs"){ log =>
    for {
      x <- random
      _ <- log.info(s"x : $x")
      y <- random
      _ <- log.info(s"y : $y")
    } yield expect(x != y)
  }

}
```

#### IOSuite

```scala
import weaver.IOSuite
import cats.effect._

// Same as SimpleIOSuite, but supports sharing a resource across tests
object MySuite extends IOSuite {

  override type Res = AmazonDynamodb
  def sharedResource : Resource[IO, AmazonDynamodb] = Resource.make(...)

  // A test that uses the shared resource
  test("hello resource"){ (ddb : AmazonDynamodb, log : Log[IO]) =>
    // ...
  }


}
```

### Expectations (assertions)

#### Building expectations

The various `test` functions have in common that they expect the developer to return a value of type `Expectations`, which is just a basic case class wrapping a `cats.data.Validated` value.

The most convenient way to build `Expectations` is to use the `expect` function. It captures the boolean expression at compile time and provides useful feedback on what goes wrong when it does :

![Oops](docs/assets/oops.png)

Nothing prevents the user from building their own expectations functions to resemble what they're used to.

#### Composing expectations

Something worth noting is that expectations are not throwing, and that if the user wants to perform several checks in the same test, he needs to compose the expectations via the `and` or the `or` methods they carry.

### Filtering tests

When using the IOSuite variants, the user can call the test command as such:

``` 
> test -o *foo*
```

This will filter prevent the execution of any test that doesn't contain the string "foo" in is qualified name. For a test labeled "foo" in a "FooSuite" object, in the package "fooPackage", the qualified name of a test is :

```
fooPackage.FooSuite.foo
```

### Suites (ZIO)

Weaver provides a zio module that contains ZIO-specific suites.

The main difference is that it leverages ZIO's contravariant type parameter to declare dependencies to the logger / the shared resource. Therefore, the `loggedTest` and `simpleTest` methods are non-existent, as they are encompassed by the `test` method.

```scala
import zio._
import weaver.zio._

object MyZIOSuite extends ZIOSuite {

  override type Res = DynamodbModule
  def sharedResource : Managed[Throwable, DynamodbModule] = Managed.make(...)

  // A test for non-effectful (pure) functions
  pureTest("hello pure"){
    expect("hello".size == 6)
  }

  val random = ZIO(java.util.UUID.randomUUID())

  // A test for side-effecting functions
  test("hello side-effects") {
    for {
      x <- random
      y <- random
    } yield expect(x != y)
  }

  // A test with logs
  test("hello logs"){
    for {
      x <- random
      _ <- log.info(s"x : $x")
      y <- random
      _ <- log.info(s"y : $y")
    } yield expect(x != y)
  }

    // Test that uses the shared resource
  test("hello resource"){
    for {
      x <- dynamodb.get("key")
    } yield expect(x.nonEmpty)
  }

}
```

NB : if you don't care for a `sharedResource`, just extend `weaver.zio.SimpleZIOSuite`.


### Running suites in standalone

It is possible to run suites outside of your build tool, via a good old `main` function. To do so, you can instantiate the `weaver.Runner`, create a `fs2.Stream` of the suites you want to run, and call `runner.run(stream)`.

This is useful when you consider your tests (typically `end-to-end` ones) as a program of its own and want to avoid paying the cost of compiling them every time you run them.

### Scalacheck (property-based testing)

Weaver comes with basic scalacheck integration.

```scala
import weaver._
import weaver.scalacheck._

// Notice the IOCheckers mix-in
object ForallExamples extends SimpleIOSuite with IOCheckers {

  simpleTest("Gen form") {
    // Takes an explicit "Gen" instance. There is only a single
    // version of this overload. If you want to pass several Gen instances
    // at once, just compose them monadically.
    forall(Gen.posNum[Int]) { a =>
      expect(a > 0)
    }
  }

  simpleTest("Arbitrary form") {
    // Takes a number of implicit "Arbitrary" instances. There are 6 overloads
    // to pass 1 to 6 parameters.
    forall { (a1: Int, a2: Int, a3: Int) =>
      expect(a1 * a2 * a3 == a3 * a2 * a1)
    }
  }

}
```

## Contributing

Contributions are most welcome ! 

## Inspiration

A **HUGE** thank you to Alexandru Nedelcu, author of [Monix](https://github.com/monix/monix) and contributor to
cats-effect, as he wrote the [minitest](https://github.com/monix/minitest)
framework which got this framework started.




