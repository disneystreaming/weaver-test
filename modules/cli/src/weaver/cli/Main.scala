package weaver
package cli

import java.io.PrintStream

import cats.effect.{ Blocker, ExitCode, IO }
import cats.implicits._

import weaver.codecs._

import com.monovore.decline._
import com.monovore.decline.effect.CommandIOApp
import fs2.concurrent.Queue

object Main
    extends CommandIOApp("weaver-test-cli", "Runs tests from the classpath") {
  self =>

  type EventQueue = Queue[IO, SuiteEvent]

  def main: Opts[IO[ExitCode]] = {
    Args.args.map {
      case Args(qualifiedName, only, line) =>
        Blocker[IO].use { blocker =>
          val oArg = only.foldMap(o => List("-o", o))
          val lArg = line.foldMap(l => List("-l", line.toString()))
          val args = oArg ++ lArg

          val loadSuite =
            weaver.framework.suiteFromModule(qualifiedName,
                                             self.getClass().getClassLoader())

          def reset(out: PrintStream): IO[Unit] = IO(System.setOut(out))

          for {
            stdout <- IO(System.out)
            _      <- IO(System.setOut(new PrintStream(System.err, true)))
            suite  <- loadSuite
            _      <- runSuite(suite, args, stdout).guarantee(reset(stdout))
          } yield ExitCode.Success
        }

    }
  }

  def runSuite(
      suite: EffectSuite[Any],
      args: List[String],
      printStream: PrintStream): IO[Unit] = for {
    _ <- printEvent(printStream)(SuiteStarts(suite.name))
    _ <- suite.run(args) { outcome =>
      printEvent(printStream)(TestData.fromTestOutcome(outcome))
    }
    _ <- printEvent(printStream)(SuiteEnds(suite.name))
  } yield ()

  def printEvent(printStream: PrintStream)(event: SuiteEvent) =
    IO(printStream.println(event.jsonString))

}

case class Args(qualifiedName: String, only: Option[String], line: Option[Int])

object Args {

  val qualifiedName =
    Opts.argument[String]("The full qualified name of test to run")

  val only: Opts[Option[String]] =
    Opts.option[String]("only",
                        "a wildcard-able name filter for tests",
                        short = "o").orNone

  val line: Opts[Option[Int]] =
    Opts.option[Int]("line",
                     "the line number of the test(s) to run",
                     short = "l").orNone

  val args = (qualifiedName, only, line).mapN(Args.apply)

}
