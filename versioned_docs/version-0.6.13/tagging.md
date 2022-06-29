---
id: version-0.6.13-tagging
title: Tagging
original_id: tagging
---

Weaver provides some constructs to dynamically tag tests as `ignored` or `cancelled` :

```scala
import weaver._
import cats.effect.IO
import cats.syntax.all._

object TaggingSuite extends SimpleIOSuite {

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

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>TaggingSuite</span>
<span style='color: red'>-&nbsp;</span>Only&nbsp;on&nbsp;CI&nbsp;<span style='color: lightgray'><b>15ms</span></b>
<span style='color: red'>-&nbsp;</span>Another&nbsp;on&nbsp;CI&nbsp;<span style='color: lightgray'><b>8ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>TaggingSuite</span>
<span style='color: red'>-&nbsp;</span>Only&nbsp;on&nbsp;CI&nbsp;<span style='color: lightgray'><b>15ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(tagging.md:25)<br /><br />&nbsp;&nbsp;}&nbsp;yield&nbsp;expect(x&nbsp;==&nbsp;y)<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;|&nbsp;&nbsp;2<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;false</span>
<span style='color: red'>-&nbsp;</span>Another&nbsp;on&nbsp;CI&nbsp;<span style='color: lightgray'><b>8ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(tagging.md:34)<br /><br />&nbsp;&nbsp;}&nbsp;yield&nbsp;expect(x&nbsp;==&nbsp;y)<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;1&nbsp;|&nbsp;&nbsp;2<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;false</span>

Total&nbsp;2,&nbsp;Failed&nbsp;2,&nbsp;Passed&nbsp;0,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
