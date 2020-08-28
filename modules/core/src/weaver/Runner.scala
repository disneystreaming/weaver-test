package weaver

import cats.Monoid
import cats.data.Chain
import cats.effect._
import cats.effect.concurrent.{ MVar, Ref }
import cats.implicits._

import TestOutcome.{ Summary, Verbose }

class Runner[F[_]: Concurrent](args: List[String], maxConcurrentSuites: Int)(
    printLine: String => F[Unit]) {

  import Runner._

  // Signaling option, because we need to detect completion
  type Channel[A] = MVar[F, Option[A]]

  def run(suites: fs2.Stream[F, Suite[F]]): F[Outcome] =
    for {
      channel  <- MVar.empty[F, Option[SpecEvent]]
      buffer   <- Ref[F].of(Chain.empty[SpecEvent])
      consumer <- Concurrent[F].start(consume(channel, buffer, Outcome.empty))
      _ <- suites
        .parEvalMap(math.max(1, maxConcurrentSuites)) { suite =>
          suite
            .spec(args)
            .compile
            .toList
            .map(SpecEvent(suite.name, _))
            .flatMap(produce(channel))
        }
        .compile
        .drain
      _       <- complete(channel)
      outcome <- consumer.join
    } yield outcome

  def produce(ch: Channel[SpecEvent])(event: SpecEvent): F[Unit] =
    ch.put(Some(event))

  def complete(channel: Channel[SpecEvent]): F[Unit] =
    channel.put(None) // We are done !

  // Recursively consumes from a channel until a "None" gets produced,
  // indicating the end of the stream.
  def consume(
      ch: Channel[SpecEvent],
      buffer: Ref[F, Chain[SpecEvent]],
      outcome: Outcome): F[Outcome] = {

    val stars = "*************"

    def newLine = printLine("")
    def printTestEvent(mode: TestOutcome.Mode)(event: Event) =
      printLine(event.formatted(mode))
    def handle(specEvent: SpecEvent): F[Outcome] = {
      val (successes, failures, outcome) =
        specEvent.events.foldMap[(List[Event], List[Event], Outcome)] {
          case ev if ev.status.isFailed =>
            (List.empty, List(ev), Outcome.fromEvent(ev))
          case ev => (List(ev), List.empty, Outcome.fromEvent(ev))
        }

      for {
        _ <- printLine(cyan(specEvent.name))
        _ <- (successes ++ failures).traverse(printTestEvent(Summary))
        _ <- newLine
        _ <- buffer
          .update(_.append(specEvent.copy(events = failures)))
          .whenA(failures.nonEmpty)
      } yield outcome
    }

    ch.take.flatMap {
      case Some(specEvent) =>
        for {
          specOutcome    <- handle(specEvent)
          updatedOutcome <- (outcome |+| specOutcome).pure[F]
          // next please
          res <- consume(ch, buffer, updatedOutcome)
        } yield res

      case None =>
        // We're done !
        for {
          failures <- buffer.get
          _ <- (printLine(red(stars) + "FAILURES" + red(stars)) *> failures
            .traverse[F, Unit] { specEvent =>
              printLine(cyan(specEvent.name)) *>
                specEvent.events.traverse(printTestEvent(Verbose)) *>
                newLine
            }
            .void).whenA(failures.nonEmpty)
          _ <- printLine(outcome.formatted)
        } yield outcome
    }
  }

}

object Runner {

  case class SpecEvent(name: String, events: List[Event])

  case class Outcome(
      successes: Int,
      ignored: Int,
      cancelled: Int,
      failures: Int) { self =>

    def total = successes + ignored + cancelled + failures

    def formatted: String =
      s"Total $total, Failed $failures, Passed $successes, Ignored $ignored, Cancelled $cancelled"

  }

  object Outcome {
    val empty = Outcome(0, 0, 0, 0)

    def fromEvent(event: Event): Outcome = event.status match {
      case TestStatus.Exception =>
        Outcome(0, 0, 0, failures = 1)
      case TestStatus.Failure =>
        Outcome(0, 0, 0, failures = 1)
      case TestStatus.Success =>
        Outcome(successes = 1, 0, 0, 0)
      case TestStatus.Ignored =>
        Outcome(0, ignored = 1, 0, 0)
      case TestStatus.Cancelled =>
        Outcome(0, 0, cancelled = 1, 0)
    }

    implicit val monoid: Monoid[Outcome] = new Monoid[Outcome] {
      override def empty = Outcome.empty

      override def combine(left: Outcome, right: Outcome) = Outcome(
        left.successes + right.successes,
        left.ignored + right.ignored,
        left.cancelled + right.cancelled,
        left.failures + right.failures
      )
    }
  }

}
