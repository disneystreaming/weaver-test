package weaver

import scala.concurrent.duration._

import io.circe.{ ACursor, Decoder, DecodingFailure, Encoder, HCursor, Json }

package object codecs {

  implicit class WeaverEncoderOps[A](encoder: Encoder[A]) {
    def oneOf[Union >: A](label: String, value: A): EncodeOneOf[Union, A] =
      EncodeOneOf(label, encoder, value)
  }

  def encodeUnion[Union](
      route: Union => EncodeOneOf[Union, _]): Encoder[Union] =
    (member: Union) => route(member).encode

  implicit class WeaverDecoderOps[A](decoder: Decoder[A]) {
    def oneOf[Union >: A](label: String): DecodeOneOf[Union, A] =
      DecodeOneOf(label, decoder)
  }

  def decodeUnion[Union](oneOfs: DecodeOneOf[Union, _]*): Decoder[Union] = {
    val emptyUnion = Decoder.failed[Union](DecodingFailure("Empty union", Nil))
    oneOfs.map(_.upcast).fold(emptyUnion)(_ or _)
  }

  def recursiveDecoder[A](f: => Decoder[A]): Decoder[A] = new Decoder[A] {
    lazy val decoder: Decoder[A]                          = f
    def apply(c: HCursor): Decoder.Result[A]              = decoder(c)
    override def tryDecode(c: ACursor): Decoder.Result[A] = decoder.tryDecode(c)
  }

  def recursiveEncoder[A](f: => Encoder[A]): Encoder[A] = new Encoder[A] {
    lazy val encoder: Encoder[A] = f
    def apply(a: A): Json        = encoder(a)
  }

  implicit val sourceLocationEncoder: Encoder[SourceLocation] =
    Encoder.forProduct3[SourceLocation, Option[String], Option[String], Int](
      "fileName",
      "filePath",
      "line"
    )(sl => (sl.fileName, sl.filePath, sl.line))

  implicit val sourceLocationDecoder: Decoder[SourceLocation] =
    Decoder.forProduct3[SourceLocation, Option[String], Option[String], Int](
      "fileName",
      "filePath",
      "line"
    )(SourceLocation.apply)

  implicit val logLevelEncoder: Encoder[Log.Level] =
    Encoder.encodeString.contramap(level => level.label)

  implicit val logLevelDecoder: Decoder[Log.Level] =
    Decoder.decodeString.emap(Log.Level.fromString)

  implicit val testStatusEncoder: Encoder[TestStatus] =
    Encoder.encodeString.contramap(_.label)

  implicit val testStatusDecoder: Decoder[TestStatus] =
    Decoder.decodeString.emap(TestStatus.fromString)

  implicit val finiteDurationEncoder: Encoder[FiniteDuration] =
    Encoder.encodeLong.contramap(_.toMillis)

  implicit val finiteDurationDecoder: Decoder[FiniteDuration] =
    Decoder.decodeLong.map(_.millis)

}
