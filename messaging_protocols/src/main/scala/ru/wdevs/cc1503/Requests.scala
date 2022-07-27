package ru.wdevs.cc1503
import io.circe.{Decoder, Encoder}
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import ru.wdevs.cc1503.Responses.MessagingResponseDTO
import sttp.tapir.{Codec, CodecFormat, Schema}
import sttp.ws.WebSocketFrame
object Requests {

  implicit val en: Encoder[MessagingRequestDTO] = deriveEncoder
  implicit val dec: Decoder[MessagingRequestDTO] = deriveDecoder

  sealed trait MessagingRequestDTO

  case class CreateMessageDTO(text: String, channelId: String) extends MessagingRequestDTO
  case class FetchMessagesDTO(channelId: String) extends MessagingRequestDTO
}
