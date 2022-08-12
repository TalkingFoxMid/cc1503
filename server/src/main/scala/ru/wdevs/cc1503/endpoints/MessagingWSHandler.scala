package ru.wdevs.cc1503.endpoints

import cats.effect.kernel.Async
import cats.effect.{Concurrent, Sync}
import fs2.{Pipe, Pull, Stream}
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
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.MessageAnnouncer
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.endpoints.MessagingWSHandler.RequestWithMetadata

class MessagingWSHandler[F[_]: Async: Logger](
  ms: MessageStore[F],
  announcer: MessageAnnouncer[F],
  subscribers: ChatSubscribersRepository[F]
) extends WSHandler[F, MessagingRequestDTO, MessagingResponseDTO] {
  override val _async: Async[F] = Async[F]

  override val wsEndpoint: PublicEndpoint[Unit, Unit, Pipe[F, MessagingRequestDTO, MessagingResponseDTO], Fs2Streams[
    F
  ] with capabilities.WebSockets] =
    endpoint.get
      .in("messaging")
      .out(webSocketBody[MessagingRequestDTO, CodecFormat.Json, MessagingResponseDTO, CodecFormat.Json](Fs2Streams[F]))

  private def processRequest(req: MessagingRequestDTO, session: Option[String]): F[MessagingResponseDTO] =
    (req, session) match {
      case (InitSession(uuid), Some(session)) => RequestError("Session already exists").pure[F].widen
      case (InitSession(uuid), None) => SessionWasInitialized().pure[F].widen
      case (SubscribeChat(chatId), Some(session)) =>
        subscribers.subscribeChat(Channel.Id(chatId), session)
          .as(SubscribedToChat(chatId))

      case (ReadMessages(chatId, limit), Some(session)) =>
        ms.getMessages(Channel.Id(chatId), limit)
          .map(
            msgs => MessageHistory(chatId, msgs.map(msg => ChatMessage(msg.text, msg.author)))
          )

      case (CreateMessageDTO(channelId, text), Some(session)) =>
        for {
          _ <- ms.saveMessage(text, Channel.Id(channelId), session)
          _ <- announcer.announce(Channel.Id(channelId), text)
          _ <- Logger[F].info(s"Message was saved by $session")
        } yield MessageSaved()
      case (_, None) => RequestError("You need to initialize session").pure[F].widen
    }

  private def processStream(s: Stream[F, MessagingRequestDTO]): Stream[F, MessagingResponseDTO] = {
    def processStreamRec(s: Stream[F, RequestWithMetadata]): Stream[F, MessagingResponseDTO] = {
      s.pull.uncons1.flatMap {
        case Some((req @ RequestWithMetadata(InitSession(uuid), None), remain)) =>
          val response = processRequest(req.req, None)
          for {
            _ <- Stream.eval(response).pull.echo
            userChats <- Pull.eval(subscribers.userChats(uuid))
            _ <- {
              processStreamRec(remain.map(r => RequestWithMetadata(r.req, Some(uuid))))
                .merge(
                  announcer.subscribe(userChats)
                    .map(e => IncomingMessage(e.chatId.id, e.text))
                )
            }
              .pull.echo
          } yield ()

        case Some((RequestWithMetadata(SubscribeChat(chatId), Some(_)), remain)) => {
          announcer.subscribe(Channel.Id(chatId) :: Nil)
            .map(e => IncomingMessage(e.chatId.id, e.text))
            .merge(processStreamRec(remain))
        }.pull.echo

        case Some((RequestWithMetadata(req, session), remain)) =>
          val response = processRequest(req, session)
          Stream.eval(response).pull.echo >> processStreamRec(remain).pull.echo


        case None => Pull.done
      }.stream
    }
    processStreamRec(s.map(RequestWithMetadata(_, None)))
  }

  override val pipe: Pipe[F, MessagingRequestDTO, MessagingResponseDTO] = processStream
}

object MessagingWSHandler {
  case class RequestWithMetadata(req: MessagingRequestDTO, session: Option[String])
}
