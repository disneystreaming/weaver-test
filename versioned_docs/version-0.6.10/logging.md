---
id: version-0.6.10-logging
title: Logging information
original_id: logging
---

Weaver provides each individual test with a lazy-logger. The log statements only get reported if the test is unsuccessful. Because tests in weaver run in parallel by default, this makes it easier to tie printed information to the test it originated from.

```scala
import weaver._
import cats.effect._

object LoggedTests extends IOSuite {

  // Only the logger is received as an argument
  loggedTest("Just logging some stuff") { log =>
    for {
      _ <- log.info("oopsie daisy")
    } yield expect(2 + 2 == 5)
  }


  // We can obviously have tests receive loggers AND shared resources
  override type Res = String
  override def sharedResource : Resource[IO, Res] =
    Resource.pure[IO, Res]("hello")

  // Both the logger and the resource are received as arguments
  test("Good requests lead to good results") { (sharedString, log) =>
    for {
      _ <- log.info(sharedString)
    } yield expect(2 + 2 == 4)
  }

}
```

<div class='terminal'><pre><code class = 'nohighlight'>
<span style='color: cyan'>LoggedTests</span>
<span style='color: green'>+&nbsp;</span>Good&nbsp;requests&nbsp;lead&nbsp;to&nbsp;good&nbsp;results&nbsp;<span style='color: lightgray'><b>9ms</span></b>
<span style='color: red'>-&nbsp;</span>Just&nbsp;logging&nbsp;some&nbsp;stuff&nbsp;<span style='color: lightgray'><b>11ms</span></b>

<span style='color: red'>*************</span>FAILURES<span style='color: red'>*************</span>
<span style='color: cyan'>LoggedTests</span>
<span style='color: red'>-&nbsp;</span>Just&nbsp;logging&nbsp;some&nbsp;stuff&nbsp;<span style='color: lightgray'><b>11ms</span></b><br /><span style='color: red'>&nbsp;&nbsp;assertion&nbsp;failed&nbsp;(logging.md:20)<br /><br />&nbsp;&nbsp;}&nbsp;yield&nbsp;expect(2&nbsp;+&nbsp;2&nbsp;==&nbsp;5)</span><br /><br />&nbsp;&nbsp;&nbsp;&nbsp;[INFO]&nbsp;13:04:15&nbsp;[logging.md:19]&nbsp;oopsie&nbsp;daisy

Total&nbsp;2,&nbsp;Failed&nbsp;1,&nbsp;Passed&nbsp;1,&nbsp;Ignored&nbsp;0,&nbsp;Cancelled&nbsp;0
</code></pre></div>
