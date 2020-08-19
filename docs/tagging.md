---
id: tagging
title: Tagging
---

Weaver provides some constructs to dynamically tag tests as `ignored` or `cancelled` :   

```scala mdoc
import cats.effect._


object MySuite extends SimpleIOSuite {


  simpleTest("hello tag ignore") {
    for {
      x <- IO.delay(1)
      y <- IO.delay(2)
    } yield expect(x == y)
    ignore("I want to ignore this test because is flaky")
  }

    simpleTest("hello tag cancel") {
    for {
      x <- IO.delay(1)
      y <- IO.delay(2)
    } yield expect(x == y)
    cancel("I want to cancel the test")
  }

}
```
