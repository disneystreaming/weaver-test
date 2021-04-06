---
id: funsuite
title: Pure tests
---

If your tests don't perform and effects, you can use a simplified interface `FunSuite`,
which comes with a single `test` method and does not allow effectful tests.

Tests in `FunSuite` are executed sequentially and without the performance overhead of effect
management.


```scala mdoc
object CatsFunSuite extends weaver.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }

  test("fails")   { expect(Some(25).contains(5)) }

  test("throws")  { throw new RuntimeException("oops") }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(CatsFunSuite).unsafeRunSync())
```

A `FunSuite` alias is provided in each of the frameworks supported by weaver:

```scala mdoc
object MonixFunSuite extends weaver.monixcompat.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }
}

object MonixBIOFunSuite extends weaver.monixbiocompat.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }
}

object ZioBIOFunSuite extends weaver.ziocompat.FunSuite {
  test("asserts") { expect(Some(5).contains(5)) }
}
```
