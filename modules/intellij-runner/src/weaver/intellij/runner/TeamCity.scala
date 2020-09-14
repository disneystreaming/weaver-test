package weaver.intellij.runner

import java.io.{ PrintWriter, StringWriter }

import weaver.{ TestOutcome, TestStatus }

object TeamCity {

  def testOutcome(
      result: TestOutcome,
      parentId: Int,
      id: Int): List[TeamCityEvent] = {
    import result._

    def teamCity(eventName: String, attributes: (String, Any)*) =
      new TeamCityEvent(eventName, id, attributes)

    val start =
      teamCity("testStarted", "name" -> name, "parentNodeId" -> parentId)

    def error(message: String): List[TeamCityEvent] = {
      val details      = cause.fold("")(ex => s"${stackTrace(ex)}")
      val errorMessage = cause.fold(message)(_.getMessage)

      List(
        teamCity("testFailed",
                 "message"           -> errorMessage,
                 "details"           -> details,
                 "duration"          -> duration.toMillis),
        teamCity("testStdOut", "out" -> formatted(TestOutcome.Verbose))
      )
    }

    start :: (status match {
      case TestStatus.Success =>
        List(teamCity("testFinished", "duration" -> duration.toMillis))
      case TestStatus.Exception => error("Error")
      case TestStatus.Failure   => error("Failure")
      case TestStatus.Ignored =>
        List(teamCity("testIgnored",
                      "name"         -> name,
                      "parentNodeId" -> parentId))
      case TestStatus.Cancelled =>
        List(teamCity("testFailed", "message" -> "Cancelled"))
    })
  }

  def suiteStarted(id: Int, testClass: String): TeamCityEvent =
    TeamCityEvent("testSuiteStarted",
                  id,
                  "parentNodeId" -> 0,
                  "name"         -> testClass)

  def suiteFinished(id: Int, testClass: String): TeamCityEvent =
    TeamCityEvent("testSuiteFinished",
                  id,
                  "parentNodeId" -> 0,
                  "name"         -> testClass)

  private def stackTrace(throwable: Throwable): String = {
    val sw = new StringWriter
    val pw = new PrintWriter(sw, true)
    throwable.printStackTrace(pw)
    sw.getBuffer.toString
  }

}
