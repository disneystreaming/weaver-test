---
id: intellij
title: intellij integration
---

Starting with version 0.6.0, weaver provides intellij integration by means of a JUnit runner that Intellij picks up automatically.

## Note regarding the previous intellij integration

We (the maintainers) had tried to build an IntelliJ plugin. It worked but its maintenance became problematic quickly for us. We have made the decision to deprecate that plugin, sacrificing a little bit of UX in favour of an approach that is more compatible with our time constraints.

## Installation

Nothing is needed (as long as weaver is declared correctly in your build).

## Usage

### Running suites

When test suites are open in Intellij, buttons appear to the left of the editor (next to the line number of the suite declaration), letting you run individual suites from the IDE.

![](../img/intellij_usage.png)

### Running individual tests

A `.only` extension method is provided on strings, and can be used when declaring tests. When at least one test is "tagged" as such in a suite, weaver will ignore all tests but the ones that have the "only" tag.

```scala mdoc  
import weaver._
import cats.effect._

object MySuite extends SimpleIOSuite {

  test("test this".only) {
    IO(success)
  }

  test("do not test this") {
    IO.raiseError(new Throwable("Boom"))
  }

}
```

### Note regarding test durations

Because of inherently modelling incompatiblities between weaver and Intellij, we had to implement the JUnit runner in a way that makes it impossible for durations to be reported correctly by the IDE. We apologise for the inconvenience.
