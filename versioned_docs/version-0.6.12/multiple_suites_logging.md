---
id: version-0.6.12-multiple_suites_logging
title: Logging
original_id: multiple_suites_logging
---

Weaver only outputs the logs for tests that failed - the logs are neatly collected alongside the failure information and reported after all the tests have run.

Additionally, each log can have a context associated with it - which gets printed alongside the message.

```scala
import weaver._
import cats.effect._

object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  loggedTest("logging for success") { log =>
    for {
      x <- randomUUID
      y <- randomUUID
      _ <- log.info(s"Generated $x and $y")
    } yield expect(x != y)
  }

}

object MyAnotherSuite extends SimpleIOSuite {
  import java.util.concurrent.TimeUnit
  import scala.util.Random.alphanumeric

  val randomString = IO(alphanumeric.take(10).mkString(""))

  loggedTest("failure should print logs") { log =>
    for {
      currentTime <- timer.clock.realTime(TimeUnit.SECONDS)
      context = Map("time" -> currentTime.toString, "purpose" -> "docs")
      _ <- log.info("Starting the test...", context)
      x <- randomString
      _ <- log.debug(s"Generated random string: $x")
    } yield expect(x.length > 20)
  }
}
```

The report would look something like this:

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>MyAnotherSuite</span>
<span style='color: red'>-&nbsp;</span>failure&nbsp;should&nbsp;print&nbsp;logs&nbsp;<span style='color: lightgray'><b>37ms</span></b>

<span style='color: cyan'>MySuite</span>
<span style='color: green'>+&nbsp;</span>logging&nbsp;for&nbsp;success&nbsp;<span style='color: lightgray'><b>51ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>MyAnotherSuite</span>
<span style='color: red'>-&nbsp;</span>failure&nbsp;should&nbsp;print&nbsp;logs&nbsp;<span style='color: lightgray'><b>37ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(multiple_suites_logging.md:42)<br /><br />&nbsp;&nbsp;}&nbsp;yield&nbsp;expect(x.length&nbsp;>&nbsp;20)<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;|&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|&nbsp;10&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;false<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;4prKjG5ws0</span><br /><br />&nbsp;&nbsp;&nbsp;&nbsp;[INFO]&nbsp;&nbsp;09:00:19&nbsp;[multiple_suites_logging.md:39]&nbsp;Starting&nbsp;the&nbsp;test...<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;time&nbsp;&nbsp;&nbsp;&nbsp;->&nbsp;1653296419<br />&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;purpose&nbsp;->&nbsp;docs<br />&nbsp;&nbsp;&nbsp;&nbsp;[DEBUG]&nbsp;09:00:19&nbsp;[multiple_suites_logging.md:41]&nbsp;Generated&nbsp;random&nbsp;string:&nbsp;4prKjG5ws0

Total&nbsp;2,&nbsp;Failed&nbsp;1,&nbsp;Passed&nbsp;1,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
