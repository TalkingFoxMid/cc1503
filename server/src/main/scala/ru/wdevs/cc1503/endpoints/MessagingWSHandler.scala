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
          .flatMap(
            _ => {
              println(s"$session subscribed on $chatId")
              Logger[F].info(s"$session subscribed on $chatId")
            }
          )
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

  private def processRequestStream(req: MessagingRequestDTO, session: Option[String]): Stream[F, MessagingResponseDTO] = {
    (req, session) match {
      case (InitSession(uuid), Some(session)) => Pull.output1(RequestError("Session already exists"))
      case (InitSession(uuid), None) => for {
        userChats <- Pull.eval(subscribers.userChats(uuid))
        _ <- Stream.emit(SessionWasInitialized())
          .merge {
            announcer.subscribe(userChats)
              .map(e => IncomingMessage(e.chatId.id, e.text))
          }.pull.echo
      } yield ()
      case (SubscribeChat(chatId), Some(session)) =>
        println("OMG")
        for {
          _ <- Pull.eval(subscribers.subscribeChat(Channel.Id(chatId), session))
          _ <- Stream.emit(SubscribedToChat(chatId))
            .merge {
              announcer.subscribe(Channel.Id(chatId) :: Nil)
                .map(e => IncomingMessage(e.chatId.id, e.text))
            }.pull.echo
        } yield ()

      case (ReadMessages(chatId, limit), Some(session)) =>
        for {
          msgs <- Pull.eval(ms.getMessages(Channel.Id(chatId), limit))
          _ <- Pull.output1(MessageHistory(chatId, msgs.map(msg => ChatMessage(msg.text, msg.author))))
        } yield ()

      case (CreateMessageDTO(channelId, text), Some(session)) =>
        for {
          _ <- Pull.eval(ms.saveMessage(text, Channel.Id(channelId), session))
          _ <- Pull.eval(announcer.announce(Channel.Id(channelId), text))
          _ <- Pull.eval(Logger[F].info(s"Message was saved by $session"))
          _ <- Pull.output1(MessageSaved())
        } yield ()
      case (_, None) => Pull.output1(RequestError("You need to initialize session"))
    }
  }.stream

  private def processStream(s: Stream[F, MessagingRequestDTO]): Stream[F, MessagingResponseDTO] = {
    def processStreamRec(s: Stream[F, RequestWithMetadata]): Stream[F, MessagingResponseDTO] = {
      s.pull.uncons1.flatMap {
        case Some((RequestWithMetadata(req @ InitSession(uuid), None), remain)) =>
          processRequestStream(req, None)
            .merge(processStreamRec(remain.map(r => RequestWithMetadata(r.req, Some(uuid)))))
            .pull.echo

        case Some((RequestWithMetadata(req, session), remain)) =>
         processRequestStream(req, session)
           .merge(processStreamRec(remain))
           .pull.echo
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
