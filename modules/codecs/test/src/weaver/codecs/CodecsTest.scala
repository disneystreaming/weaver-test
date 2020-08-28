package weaver
package codecs

import scala.concurrent.duration._
import scala.io.Source

import cats.data.Chain
import cats.effect.IO
import cats.implicits._

import io.circe.syntax._

object CodecsTest extends SimpleIOSuite {

  val logContext = Map("a" -> "a", "b" -> "b")
  val throwable1 = new Throwable("boom1")
  val throwable2 = new Throwable("boom2")
  val cancelled =
    new CanceledException(Some("cancelled"), SourceLocation.fromContext)
  val ignored =
    new IgnoredException(Some("ignored"), SourceLocation.fromContext)

  val entries = Log.Level.values.map { level =>
    Log.Entry(timestamp = 1000L,
              msg = "log",
              ctx = logContext,
              level,
              Some(throwable1),
              SourceLocation.fromContext)
  }

  val results = List(Result.success,
                     Result.from(throwable2),
                     Result.from(cancelled),
                     Result.from(ignored))

  val outcomes = for {
    entry  <- entries
    result <- results
  } yield TestOutcome.Default("outcome",
                              1.second,
                              result,
                              Chain.fromSeq(entries))

  val messages =
    List(SuiteStarts("foo")) ++
      outcomes.map(TestData.fromTestOutcome) ++
      List(SuiteEnds("foo"))

  simpleTest(
    "golden test: previously serialised payload is still deserialisable") {
    IO(Source.fromResource("golden.json")).flatMap { reader =>
      val jsonString = reader.getLines().mkString("")
      io.circe.parser.parse(jsonString).liftTo[IO]
    }.flatMap(_.as[SuiteEvent].liftTo[IO]).as(success)
  }

  for ((message, index) <- messages.zipWithIndex)
    pureTest(s"test message $index can be round-tripped through json") {
      message.asJson.as[SuiteEvent] match {
        case Right(result) => expect(result == message)
        case Left(error)   => failure(s"Failed to decode json: ${error}")
      }
    }
}
