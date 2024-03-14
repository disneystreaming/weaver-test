---
id: faqs 
title: Frequently Asked Questions
---

## How do I get weaver working with IntelliJ / vscode / neovim ?

Weaver provides a JUnit runner that IDEs pick up automatically. On IntelliJ the suite should have the normal button to run the tests, and Metals should provide a code lens.

## How do I ignore individual tests ?

An `.ignore` extension method is provided on strings, and can be used when declaring tests. All tests that are tagged with `.ignore` will be ignored in the test suite, including any that are tagged with `.only`.

```scala mdoc  
import weaver._
import cats.effect._

object MyIgnoreSuite extends SimpleIOSuite {

  test("test this") {
    IO(success)
  }

  test("do not test this".ignore) {
    IO.raiseError(new Throwable("Boom"))
  }

}
```

## How do I run just one test in a suite? 

A `.only` extension method is provided on strings, and can be used when declaring tests. When at least one test is "tagged" as such in a suite, weaver will ignore all tests but the ones that have the "only" tag. Note: `.ignore` has precedence over `.only`.

```scala mdoc
import weaver._
import cats.effect._

object MyOnlySuite extends SimpleIOSuite {

  test("test this".only) {
    IO(success)
  }

  test("do not test this") {
    IO.raiseError(new Throwable("Boom"))
  }

}
```

### Regarding inaccurate test duration when using IntelliJ

Because of modeling incompatibilities between weaver and IntelliJ, the JUnit runner is implemented in a way that makes it impossible for individual test duration to be reported correctly by the IntelliJ's test runner. Sorry about that!
