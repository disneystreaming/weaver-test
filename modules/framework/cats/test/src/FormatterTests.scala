package weaver
package framework
package test

import scala.concurrent.duration._

object FormatterTests extends SimpleIOSuite {

  pureTest("rendering of durations") {
    val render = Formatter.renderDuration _

    expect.all(
      render(134.millis) == "134ms",
      render(25.second) == "25s",
      render(61.second) == "1:01min",
      render(25.minute) == "25min",
      render(1.minute) == "1min",
      render(150.second) == "2:30min"
    )
  }
}
