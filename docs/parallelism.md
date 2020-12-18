---
id: parallelism
title: Configuring parallelism
---

## Parallelism within suites

By default, using the documented test suites, weaver runs all tests within a suite in parallel.

It is possible to limit the the number of tests that that can be executed in parallel as such :

```scala mdoc
import weaver._


object MySuite extends SimpleIOSuite {

  override def maxParallelism = 1

}
```

## Parallelism across suites

Weaver does not make any decision as to whether suites are executed sequentially or in parallel. Instead, it is implemented in a way that respects the build tool settings.

#### SBT

* [Parallel execution](https://www.scala-sbt.org/1.x/docs/Parallel-Execution.html#Background)
* [Disabling parallel execution in tests](https://www.scala-sbt.org/1.x/docs/Combined+Pages.html#Disable+Parallel+Execution+of+Tests)
* [Tests are executed sequentially in forked JVMs](https://www.scala-sbt.org/1.x/docs/Testing.html#Forking+tests)

#### Mill

* [Parallel task execution](http://www.lihaoyi.com/mill/#parallel-task-execution-experimental)

#### Bloop

* [bloop test --parallel](https://scalacenter.github.io/bloop/docs/cli/reference#bloop-test)
