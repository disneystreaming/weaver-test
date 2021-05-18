---
id: version-0.6.3-multiple_suites_success
title: Successes
original_id: multiple_suites_success
---

Start with importing the following :

```scala
import weaver._
```

```scala
import cats.effect._

object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  test("hello side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect(x != y)
  }

}

object MyAnotherSuite extends SimpleIOSuite {
  import scala.util.Random.alphanumeric

  val randomString = IO(alphanumeric.take(10).mkString(""))

  test("double reversing is identity") {
    for {
      x <- randomString
    } yield expect(x == x.reverse.reverse)
  }

}
```

The report looks like this:

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>MySuite</span>
<span style='color: green'>+&nbsp;</span>hello&nbsp;side-effects&nbsp;<span style='color: lightgray'><b>11ms</span></b>

<span style='color: cyan'>MyAnotherSuite</span>
<span style='color: green'>+&nbsp;</span>double&nbsp;reversing&nbsp;is&nbsp;identity&nbsp;<span style='color: lightgray'><b>15ms</span></b>

Total&nbsp;2,&nbsp;Failed&nbsp;0,&nbsp;Passed&nbsp;2,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
