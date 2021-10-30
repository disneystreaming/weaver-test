package weaver
package intellij.runner

import java.util.concurrent.atomic.AtomicInteger

import weaver._

object WeaverTestRunner { self =>

  def main(args: Array[String]): Unit = {
    val config = parse(args.toList)
    validate(config)
    val idCounter = new AtomicInteger(1)
    val testClassesWithId = config.testClasses.map { testClass =>
      val id = idCounter.getAndIncrement()
      id -> testClass
    }
    testClassesWithId.foreach { case (parentId, testClass) =>
      report(TeamCity.suiteStarted(parentId, testClass))
      run(testClass, config.testName) { outcome =>
        val nodeId = idCounter.getAndIncrement()
        TeamCity.testOutcome(outcome, parentId, nodeId).foreach(report)
      }
      report(TeamCity.suiteFinished(parentId, testClass))
    }
  }

  private def run(name: String, testName: Option[String])(
      report: TestOutcome => Unit
  ): Unit = {
    import weaver.internals.Reflection._
    val suite =
      cast[RunnableSuite[Any]](loadModule(name,
                                          self.getClass().getClassLoader()))
    val args =
      testName.fold[List[String]](Nil)(n => "-o" :: ((name + "." + n) :: Nil))
    suite.runUnsafe(args)(report)
  }

  def report(event: TeamCityEvent): Unit = println(event.show)

  case class Config(
      testClasses: List[String],
      showProgressMessages: Boolean, // What is this supposed to do?
      testName: Option[String])

  private def parse(
      args: List[String],
      config: Config = Config(Nil, false, None)): Config =
    args match {
      case "-s" :: testClass :: rest =>
        parse(rest, config.copy(testClasses = config.testClasses :+ testClass))
      case "-testName" :: testName :: rest =>
        parse(rest, config.copy(testName = Some(testName)))
      case "-showProgressMessages" :: "true" :: rest =>
        parse(rest, config.copy(showProgressMessages = true))
      case "-showProgressMessages" :: "false" :: rest =>
        parse(rest, config.copy(showProgressMessages = false))
      case Nil => config
      case _ =>
        throw new IllegalArgumentException(
          s"Invalid arguments in [${args.mkString(" ")}]")
    }

  def validate(config: Config): Unit =
    if (config.testName.isDefined && config.testClasses.size != 1) {
      throw new UnsupportedOperationException(
        "testName can only be used with exactly one testClass"
      )
    }

}
