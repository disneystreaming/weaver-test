package weaver.codecs

import io.circe.Encoder
import io.circe.Json
import io.circe.Decoder

case class EncodeOneOf[Union, Member <: Union](
    label: String,
    memberEncoder: Encoder[Member],
    member: Member) {
  def encode: Json = Json.obj(label -> memberEncoder(member))
}

case class DecodeOneOf[Union, Member <: Union](
    label: String,
    memberDecoder: Decoder[Member]
) {
  def upcast: Decoder[Union] = memberDecoder.at(label).map(identity)
}
