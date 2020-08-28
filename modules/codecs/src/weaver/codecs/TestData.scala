package weaver.codecs

import scala.concurrent.duration.FiniteDuration
import weaver.TestStatus
import cats.data.Chain
import weaver.Log
import weaver.TestOutcome
import io.circe._

sealed trait SuiteEvent
object SuiteEvent {
  implicit val suiteEventEncoder = encodeUnion[SuiteEvent] {
    case s: SuiteStarts => SuiteStarts.suiteStartsEncoder.oneOf("start", s)
    case s: SuiteEnds   => SuiteEnds.suiteEndsEncoder.oneOf("end", s)
    case o: TestData    => TestData.testDataEncoder.oneOf("outcome", o)
  }

}

case class SuiteStarts(name: String) extends SuiteEvent
object SuiteStarts {
  implicit val suiteStartsEncoder: Encoder[SuiteStarts] =
    Encoder.forProduct1[SuiteStarts, String]("name")(suiteStarts =>
      suiteStarts.name)

  implicit val suiteEndsDecoder: Encoder[SuiteEnds] =
    Encoder.forProduct1[SuiteEnds, String]("name")(suiteStarts =>
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
    log: Chain[Log.Entry],
    description: String,
    cause: Option[Throwable])
    extends SuiteEvent {
  def json: Json         = TestData.testDataEncoder(this)
  def jsonString: String = json.noSpaces
}

object TestData {

  def fromTestOutcome(testOutcome: TestOutcome): TestData = TestData(
    testOutcome.name,
    testOutcome.duration,
    testOutcome.status,
    testOutcome.log,
    testOutcome.formatted(TestOutcome.Verbose),
    testOutcome.cause
  )

  implicit val testDataEncoder: Encoder[TestData] =
    Encoder.forProduct6[TestData,
                        String,
                        FiniteDuration,
                        TestStatus,
                        Chain[Log.Entry],
                        String,
                        Option[Throwable]]("name",
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
                        Chain[Log.Entry],
                        String,
                        Option[Throwable]]("name",
                                           "duration",
                                           "status",
                                           "log",
                                           "description",
                                           "cause")(TestData.apply)

}
