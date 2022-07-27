package ru.wdevs.cc1503.endpoints

import cats.effect.Sync
import fs2.{Pipe, Stream}
import io.circe.{Decoder, Encoder}
import sttp.capabilities
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.{CodecFormat, PublicEndpoint, endpoint, webSocketBody}
import sttp.tapir._
import ru.wdevs.cc1503.Requests._
import ru.wdevs.cc1503.Responses._
import ru.wdevs.cc1503.domain.Messaging.Message
import ru.wdevs.cc1503.storing.MessageStore
import sttp.ws.WebSocketFrame
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import cats.syntax.all._
import ru.wdevs.cc1503.domain.Channels.Channel

class MessagingWSHandler[F[_]: Sync](ms: MessageStore[F]) extends WSHandler[F, MessagingRequestDTO, MessagingResponseDTO] {
  override val sync: Sync[F] = Sync[F]

  override val wsEndpoint: PublicEndpoint[Unit, Unit, Pipe[F, MessagingRequestDTO, MessagingResponseDTO], Fs2Streams[F] with capabilities.WebSockets] =
    endpoint
      .get
      .in("messaging")
      .out(webSocketBody[MessagingRequestDTO, CodecFormat.Json, MessagingResponseDTO, CodecFormat.Json](Fs2Streams[F]))

  override val pipe: Pipe[F, MessagingRequestDTO, MessagingResponseDTO] =
    _.flatMap {
      case CreateMessageDTO(text, channelId) =>
        Stream.eval(
          ms.saveMessage(text, Channel.Id(channelId)).as(MessageSaved())
        )
      case FetchMessagesDTO(channelId) =>
        Stream.eval(
          for {
            storedMessages <- ms.getMessages(Channel.Id(channelId))
          } yield Messages(storedMessages.map(msg => MessageDTO(msg.text)))
        )
    }
}
