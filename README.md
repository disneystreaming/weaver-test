<img src="https://github.com/disneystreaming/weaver-test/raw/master/website/static/img/logo.png" width="200px" height="231px" align="right">


[![Build Status](https://cloud.drone.io/api/badges/disneystreaming/weaver-test/status.svg)](https://cloud.drone.io/disneystreaming/weaver-test)
[![Latest version](https://index.scala-lang.org/disneystreaming/weaver-test/weaver-core/latest.svg?color=orange)](https://index.scala-lang.org/disneystreaming/weaver-test/weaver-core)
[![Gitter](https://img.shields.io/gitter/room/disneystreaming/weaver-test.svg)](https://gitter.im/disneystreaming/weaver-test)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://scala-steward.org)
[![CLA assistant](https://cla-assistant.io/readme/badge/disneystreaming/weaver-test)](https://cla-assistant.io/disneystreaming/weaver-test)


# Weaver-test


A test-framework built on [cats-effect](https://github.com/typelevel/cats-effect) and
[fs2](https://github.com/functional-streams-for-scala/fs2), with [zio](https://zio.dev) interop.

## Installation

Weaver-test is currently published for **Scala 2.12 and 2.13**

### SBT

Refer yourself to the [releases](https://github.com/disneystreaming/weaver-test/releases) page to know the latest released version, and add the following (or scoped equivalent) to your `build.sbt` file.

```scala
libraryDependencies += "com.disneystreaming" %% "weaver-framework" % "x.y.z" % Test
testFrameworks += new TestFramework("weaver.framework.TestFramework")

// optionally (for ZIO usage)
libraryDependencies +=  "com.disneystreaming" %% "weaver-zio" % "x.y.z" % Test

// optionally (for Scalacheck usage)
libraryDependencies +=  "com.disneystreaming" %% "weaver-scalacheck" % "x.y.z" % Test

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

Weaver's core framework provides two primary types of testing suites.

| Suite name | Use case |
| --- | --- |
| `SimpleIOSuite` | Each test is a standalone `IO` action
| `IOSuite` | Each test needs access to a shared `Resource`

### Suites

#### SimpleIOSuite

The suite that is most familiar to developers :

```scala
import weaver.SimpleIOSuite
import cats.effect._

// Suites must be "objects" for them to be picked by the framework
object MySuite extends SimpleIOSuite {

  pureTest("non-effectful (pure) test"){
    expect("hello".size == 6)
  }

  private val random = IO(java.util.UUID.randomUUID())

  simpleTest("test with side-effects") {
    for {
      x <- random
      y <- random
    } yield expect(x != y)
  }

  loggedTest("test with side-effects and a logger"){ log =>
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

The `IOSuite` constructs the given resource once for all tests in the suite.

```scala
import weaver.IOSuite
import cats.effect._

object MySuite extends IOSuite {

  type Res = Int
  
  def sharedResource : Resource[IO, Int] = Resource
    .make(
      IO(println("Making resource"))
        .as(123)
    )(n => IO(println(s"Closing resource $n")))
  
  test("test, but resource not visible"){
    expect(123 == 123)
  }
  
  test("test with resource"){ n =>
    expect(n == 123)
  }
  
  test("test with resource and a logger"){ (n, log) =>
    log.info("log was available")
    expect(n == 123)
  }
}
```

#### Other suites

Weaver also includes support for `ZIO`-based suites via the optional `weaver-zio` dependency.

| Alias | Suite name | Provided by | Use case |
| --- | --- | --- | --- |
| `SimpleIOSuite`  | `SimpleMutableIOSuite`       | `weaver-framework` | Each test is a standalone `IO` action
| `IOSuite`        | `MutableIOSuite`             | `weaver-framework` | Each test needs access to a shared `Resource`
| `SimpleZIOSuite` | `SimpleMutableZIOSuite`      | `weaver-zio`       | Each test is a standalone `ZIO` action
| `ZIOSuite[R]`    | `MutableZIOSuite[R]`         | `weaver-zio`       | Each test needs access to a shared `ZLayer`

### Expectations (assertions)

#### Building expectations

The various `test` functions have in common that they expect the developer to return a value of type `Expectations`, which is just a basic case class wrapping a `cats.data.Validated` value.

The most convenient way to build `Expectations` is to use the `expect` function. Based on [Eugene Yokota's](http://eed3si9n.com/about) excellent [expecty](https://github.com/eed3si9n/expecty), it captures the boolean expression at compile time and provides useful feedback on what goes wrong when it does :

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

### Development requirements

:warning: This repository uses [Git LFS](https://git-lfs.github.com/). Having it installed is required for
`git checkout` to work properly.

To install for Mac using `brew`:

```bash
brew install git-lfs
git lfs install
```

If you want to build and run the website then you will need yarn installed:

```bash
brew install yarn
```

### PR Guidelines

Please:
- Sign the CLA
- Write positive and negative tests
- Include documentation

## Inspiration

A **HUGE** thank you to Alexandru Nedelcu, author of [Monix](https://github.com/monix/monix) and contributor to
cats-effect, as he wrote the [minitest](https://github.com/monix/minitest)
framework which got this framework started.




