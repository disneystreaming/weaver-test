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
            queue  <- fs2.concurrent.Queue.bounded[IO, SuiteEvent](1024)
            queueIO    = runQueue(queue, stdout, blocker)
            queueSuite = runSuite(suite, args, queue)
            _ <- queueIO.parProduct(queueSuite).guarantee(reset(stdout))
          } yield ExitCode.Success
        }

    }
  }

  def runSuite(
      suite: EffectSuite[Any],
      args: List[String],
      queue: EventQueue): IO[Unit] = for {
    _ <- queue.enqueue1(SuiteStarts(suite.name))
    _ <- suite.run(args) { outcome =>
      queue.enqueue1(TestData.fromTestOutcome(outcome))
    }
    _ <- queue.enqueue1(SuiteEnds(suite.name))
  } yield ()

  def runQueue(
      queue: EventQueue,
      stdout: PrintStream,
      blocker: Blocker): IO[Unit] = {

    val stdoutSink =
      fs2.io.writeOutputStream[IO](IO(stdout), blocker, closeAfterUse = false)

    val eventStream = queue.dequeue

    val pipedToSink =
      eventStream
        .takeThrough {
          case SuiteEnds(_) => false
          case _            => true
        }
        .map(_.jsonString)
        .interleave(fs2.Stream("\n").repeat)
        .through(fs2.text.utf8Encode)
        .through(stdoutSink)

    pipedToSink
      .compile
      .drain
  }

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
