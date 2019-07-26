
# Weaver-testkit

A test-framework built on [cats-effect](https://github.com/typelevel/cats-effect) and
[fs2](https://github.com/functional-streams-for-scala/fs2).

## Inspiration

A **HUGE** thank you to Alexandru Nedelcu, author of [Monix](https://github.com/monix/monix) and contributor to
cats-effect, as he wrote the [minitest](https://github.com/monix/minitest)
framework which got this framework started.


## Motivation

Weaver aims at facilitating running tests in parallel :

* suites are run in parallel
* tests within a suite are easily run in parallel
* weaver aggregates failures and reports them lazily at the end of the run


