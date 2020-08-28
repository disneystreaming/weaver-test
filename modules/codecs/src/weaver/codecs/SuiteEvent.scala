package weaver.codecs

import scala.concurrent.duration.FiniteDuration

import cats.data.Chain

import weaver.{ TestOutcome, TestStatus }

import io.circe._

sealed trait SuiteEvent extends Product with Serializable {
  def json: Json         = SuiteEvent.suiteEventEncoder(this)
  def jsonString: String = json.noSpaces
}

object SuiteEvent {

  implicit val suiteEventEncoder = encodeUnion[SuiteEvent] {
    case s: SuiteStarts => SuiteStarts.suiteStartsEncoder.oneOf("start", s)
    case s: SuiteEnds   => SuiteEnds.suiteEndsEncoder.oneOf("end", s)
    case o: TestData    => TestData.testDataEncoder.oneOf("outcome", o)
  }

  implicit val suiteEventDecoder = decodeUnion[SuiteEvent](
    SuiteStarts.suiteStartsDecoder.oneOf("start"),
    SuiteEnds.suiteEndsDecoder.oneOf("end"),
    TestData.testDataDecoder.oneOf("outcome")
  )

}

case class SuiteStarts(name: String) extends SuiteEvent
object SuiteStarts {
  implicit val suiteStartsDecoder: Decoder[SuiteStarts] =
    Decoder.forProduct1[SuiteStarts, String]("name")(SuiteStarts.apply)

  implicit val suiteStartsEncoder: Encoder[SuiteStarts] =
    Encoder.forProduct1[SuiteStarts, String]("name")(suiteStarts =>
      suiteStarts.name)
}
case class SuiteEnds(name: String) extends SuiteEvent
object SuiteEnds {
  implicit val suiteEndsEncoder: Encoder[SuiteEnds] =
    Encoder.forProduct1[SuiteEnds, String]("name")(_.name)

  implicit val suiteEndsDecoder: Decoder[SuiteEnds] =
    Decoder.forProduct1[SuiteEnds, String]("name")(SuiteEnds.apply)
}

/**
 * A serialisable view of TestOutcome
 */
case class TestData(
    name: String,
    duration: FiniteDuration,
    status: TestStatus,
    log: Chain[LogEntry],
    description: String,
    cause: Option[SerialisableThrowable])
    extends SuiteEvent

object TestData {

  def fromTestOutcome(testOutcome: TestOutcome): TestData = TestData(
    testOutcome.name,
    testOutcome.duration,
    testOutcome.status,
    testOutcome.log.map(LogEntry.fromEntry),
    testOutcome.formatted(TestOutcome.Verbose),
    testOutcome.cause.map(SerialisableThrowable.fromThrowable)
  )

  implicit val testDataEncoder: Encoder[TestData] =
    Encoder.forProduct6[TestData,
                        String,
                        FiniteDuration,
                        TestStatus,
                        Chain[LogEntry],
                        String,
                        Option[SerialisableThrowable]]("name",
                                                       "duration",
                                                       "status",
                                                       "log",
                                                       "description",
                                                       "cause") { outcome =>
      (outcome.name,
       outcome.duration,
       outcome.status,
       outcome.log,
       outcome.description,
       outcome.cause)
    }

  implicit val testDataDecoder: Decoder[TestData] =
    Decoder.forProduct6[TestData,
                        String,
                        FiniteDuration,
                        TestStatus,
                        Chain[LogEntry],
                        String,
                        Option[SerialisableThrowable]]("name",
                                                       "duration",
                                                       "status",
                                                       "log",
                                                       "description",
                                                       "cause")(TestData.apply)

}
