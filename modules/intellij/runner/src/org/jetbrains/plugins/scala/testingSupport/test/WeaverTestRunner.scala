package org.jetbrains.plugins.scala.testingSupport.test

import cats.effect.concurrent.Ref
import cats.effect.{ ExitCode, IO, IOApp }
import cats.instances.list._
import cats.syntax.parallel._
import cats.syntax.traverse._

import weaver._

object WeaverTestRunner extends IOApp {

  def run(args: List[String]): IO[ExitCode] = {
    val config = parse(args)

    if (config.testName.isDefined && config.testClasses.size != 1) {
      throw new UnsupportedOperationException(
        "testName can only be used with exactly one testClass"
      )
    }

    for {
      idCounter <- Ref.of[IO, Int](1)
      testClassesWithId <- config.testClasses.traverse { testClass =>
        for {
          id <- idCounter.modify(c => (c + 1, c))
          // TODO combine reportSuite and suiteFinished to a bracket
          _ <- WeaverTestReporter.reportSuite(id, testClass)
        } yield (id, testClass)
      }
      _ <- testClassesWithId.parTraverse_ {
        case (id, testClass) =>
          (run(testClass, config.testName)(
            WeaverTestReporter.reportScenario(id, testClass, idCounter)
          )) <* WeaverTestReporter.suiteFinished(id, testClass)
      }
    } yield ExitCode.Success
  }

  private def run(name: String, testName: Option[String])(
      report: TestOutcome => IO[Unit]
  ): IO[Unit] = {
    val suite =
      ReflectUtil.loadModule(name + "$", getClass.getClassLoader).get
    val args =
      testName.fold[List[String]](Nil)(n => "-o" :: ((name + "." + n) :: Nil))
    suite.run(args)(report)
  }

  case class Config(
      testClasses: List[String],
      showProgressMessages: Boolean, //What is this supposed to do?
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
        throw new Exception(s"Invalid arguments in [${args.mkString(" ")}]")
    }

}
