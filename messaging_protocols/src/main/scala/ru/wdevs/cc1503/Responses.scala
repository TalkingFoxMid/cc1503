package ru.wdevs.cc1503
import io.circe.{Decoder, Encoder}
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import io.circe.generic.semiauto.{deriveCodec, deriveDecoder, deriveEncoder}
import ru.wdevs.cc1503.Requests.MessagingRequestDTO
import sttp.tapir.{Codec, CodecFormat, Schema}
import sttp.ws.WebSocketFrame

object Responses {

  implicit val en: Encoder[MessagingResponseDTO] = deriveEncoder
  implicit val dec: Decoder[MessagingResponseDTO] = deriveDecoder

  sealed trait MessagingResponseDTO

  case class MessageSaved() extends MessagingResponseDTO
  case class SessionWasInitialized() extends MessagingResponseDTO
  case class RequestError(msg: String) extends MessagingResponseDTO
  case class IncomingMessage(chatId: String, text: String) extends MessagingResponseDTO
  case class SubscribedToChat(chatId: String) extends MessagingResponseDTO

  case class MessageHistory(chatId: String, messages: List[ChatMessage]) extends MessagingResponseDTO
  case class ChatMessage(text: String, author: String)
}
