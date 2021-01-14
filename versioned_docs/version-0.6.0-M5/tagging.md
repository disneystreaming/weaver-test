---
id: version-0.6.0-M5-tagging
title: Tagging
original_id: tagging
---

Weaver provides some constructs to dynamically tag tests as `ignored` or `cancelled` :

```scala
import weaver._
import cats.effect.IO
import cats.syntax.all._

object MySuite extends SimpleIOSuite {

  test("Only on CI") {
    for {
      onCI <- IO(sys.env.get("CI").isDefined)
      _    <- ignore("not on CI").unlessA(onCI)
      x    <- IO.delay(1)
      y    <- IO.delay(2)
    } yield expect(x == y)
  }

  test("Another on CI") {
    for {
      onCI <- IO(sys.env.get("CI").isDefined)
      _    <- cancel("not on CI").unlessA(onCI)
      x    <- IO.delay(1)
      y    <- IO.delay(2)
    } yield expect(x == y)
  }

}
```
