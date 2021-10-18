---
id: version-0.6.7-motivation
title: Motivation
original_id: motivation
---

## A test framework for integration tests

Weaver was built for integration/end-to-end tests. It aims at making tests faster and make issues easier to debug, by treating effect types (IO/Task/ZIO) as first-class citizens.

## History

Weaver-test was born in 2018 as an experiment, trying to speedup an extremely slow, incredibly I/O heavy test suite that was implemented with scalatest, and was making numerous http calls to real services, verifying their deployments and orchestration.

Neither **cats-effect** nor **fs2** had reached their respective 1.0.0 at the time, and **ZIO** was not yet known under that name.

Nevertheless, built on bleeding edge libraries, and offering a principled api  that didn't completely shift from more classic frameworks such as utest, specs2, scalatest, etc, **weaver** allowed to tremendously speed up the test suite in question by parallelising its test, weaving their respective computations together in a single IO value that was executed by the framework.

From there, additional problems were tackled, among which :

* Making errors appear at the very end of the report, no matter how many suites were run.
* Ensuring a principled sharing of resources across tests, using `cats.effect.Resource` to guarantee their release.
* Providing a lazy logger to enrich reporting with ad-hoc information, ensuring
it only gets displayed when a test fails.
* Treating assertions as pure values that could be composed together, negated, discarded ...


## Thank you note

A **HUGE** thank you to Alexandru Nedelcu, author of [Monix](https://github.com/monix/monix) and contributor to
cats-effect, as he wrote the [minitest](https://github.com/monix/minitest)
framework which got this framework started.

Another **HUGE** thank you to Eugene Yokota, author of [Expecty](https://github.com/eed3si9n/expecty/).

And an obvious thank you to the maintainers of cats and fs2. We stand on the shoulders of giants.
