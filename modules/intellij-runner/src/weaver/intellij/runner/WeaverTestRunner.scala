package weaver.intellij.runner

import cats.effect.concurrent.Ref
import cats.effect.{ ExitCode, IO, IOApp }
import cats.implicits._
import weaver._

object WeaverTestRunner extends IOApp { self =>

  def run(args: List[String]): IO[ExitCode] = {
    for {
      config    <- parse(args)
      _         <- validate(config)
      idCounter <- Ref.of[IO, Int](1)
      testClassesWithId <- config.testClasses.traverse { testClass =>
        idCounter.modify(id => (id + 1, id -> testClass))
      }
      _ <- testClassesWithId.parTraverse_ {
        case (parentId, testClass) =>
          for {
            _ <- report(TeamCity.suiteStarted(parentId, testClass))
            _ <- run(testClass, config.testName) { outcome =>
              idCounter.getAndUpdate(_ + 1).flatMap { nodeId =>
                TeamCity.testOutcome(outcome, parentId, nodeId)
                  .traverse(report)
                  .void
              }
            }
            _ <- report(TeamCity.suiteFinished(parentId, testClass))
          } yield ()
      }
    } yield ExitCode.Success
  }

  private def run(name: String, testName: Option[String])(
      report: TestOutcome => IO[Unit]
  ): IO[Unit] = {
    val loadSuite =
      weaver.framework.suiteFromModule(name, self.getClass().getClassLoader())
    val args =
      testName.fold[List[String]](Nil)(n => "-o" :: ((name + "." + n) :: Nil))
    loadSuite.flatMap(suite => suite.run(args)(report))
  }

  def report(event: TeamCityEvent): IO[Unit] = IO(println(event.show))

  case class Config(
      testClasses: List[String],
      showProgressMessages: Boolean, //What is this supposed to do?
      testName: Option[String])

  private def parse(
      args: List[String],
      config: Config = Config(Nil, false, None)): IO[Config] =
    args match {
      case "-s" :: testClass :: rest =>
        parse(rest, config.copy(testClasses = config.testClasses :+ testClass))
      case "-testName" :: testName :: rest =>
        parse(rest, config.copy(testName = Some(testName)))
      case "-showProgressMessages" :: "true" :: rest =>
        parse(rest, config.copy(showProgressMessages = true))
      case "-showProgressMessages" :: "false" :: rest =>
        parse(rest, config.copy(showProgressMessages = false))
      case Nil => IO.pure(config)
      case _ =>
        IO.raiseError(
          new Exception(s"Invalid arguments in [${args.mkString(" ")}]"))
    }

  def validate(config: Config): IO[Unit] =
    IO.raiseError(new UnsupportedOperationException(
      "testName can only be used with exactly one testClass"
    )).whenA(config.testName.isDefined && config.testClasses.size != 1)

}
