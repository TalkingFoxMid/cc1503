package ru.wdevs.cc1503.endpoints

import cats.effect.Sync
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
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.endpoints.MessagingWSHandler.RequestWithSession

class MessagingWSHandler[F[_]: Sync: Logger](ms: MessageStore[F]) extends WSHandler[F, MessagingRequestDTO, MessagingResponseDTO] {
  override val sync: Sync[F] = Sync[F]

  override val wsEndpoint: PublicEndpoint[Unit, Unit, Pipe[F, MessagingRequestDTO, MessagingResponseDTO], Fs2Streams[F] with capabilities.WebSockets] =
    endpoint
      .get
      .in("messaging")
      .out(webSocketBody[MessagingRequestDTO, CodecFormat.Json, MessagingResponseDTO, CodecFormat.Json](Fs2Streams[F]))

  private def processRequest(req: MessagingRequestDTO, session: Option[String]): F[MessagingResponseDTO] =
    (req, session) match {
      case (InitSession(uuid), Some(session)) => RequestError("Session already exists").pure[F].widen
      case (InitSession(uuid), None) => SessionWasInitialized().pure[F].widen
      case (CreateMessageDTO(channelId, text), Some(session)) =>
        for {
          _ <- ms.saveMessage(text, Channel.Id(channelId))
          _ <- Logger[F].info(s"Message was saved by $session")
        } yield MessageSaved()
      case (_, None) => RequestError("You need to initialize session").pure[F].widen
    }

  private def processStream(s: Stream[F, MessagingRequestDTO]): Stream[F, MessagingResponseDTO] = {
    def processStreamRec(s: Stream[F, RequestWithSession]): Stream[F, MessagingResponseDTO] = {
      s.pull.uncons1.flatMap {
        case Some((req@RequestWithSession(InitSession(uuid), None), remain)) => {
          val response = processRequest(req.req, None)
          Stream.eval(response).pull.echo >> processStreamRec(remain.map(
            r => RequestWithSession(r.req, Some(uuid))
          )).pull.echo
        }
        case Some((RequestWithSession(req, session), remain)) => {
          val response = processRequest(req, session)
          Stream.eval(response).pull.echo >> processStreamRec(remain).pull.echo
        }
        case None => Pull.done
      }.stream
    }
    processStreamRec(s.map(RequestWithSession(_, None)))
  }

  override val pipe: Pipe[F, MessagingRequestDTO, MessagingResponseDTO] = processStream
}

object MessagingWSHandler {
  case class RequestWithSession(req: MessagingRequestDTO, ses: Option[String])
}