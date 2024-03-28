---
id: ignoring
title: Ignoring tests
---

If your want to (temporarily) ignore some tests, add tag your test description with `ignore`.


```scala mdoc
object CatsFunSuite extends weaver.FunSuite {
  test("fails".ignore) { expect(Some(25).contains(5)) }

  test("throws".ignore) { throw new RuntimeException("oops") }
}
```

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(CatsFunSuite))
```
