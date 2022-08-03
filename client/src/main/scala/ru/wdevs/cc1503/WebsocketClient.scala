package ru.wdevs.cc1503

import cats.effect.{Async, IO, Ref}

import scala.concurrent.duration.FiniteDuration
import cats.syntax.all._
import ru.wdevs.cc1503.WebsocketClient._
import fs2.{Pull, Stream}
import ru.wdevs.cc1503.Requests.{CreateMessageDTO, InitSession}
import ru.wdevs.cc1503.utils.websocket.PipeParser
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.client3.{asWebSocketStream, basicRequest}
import cats.effect.unsafe.IORuntime
import fs2.{Pipe, Stream}
import sttp.capabilities.fs2.Fs2Streams
import sttp.client3._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.ws.WebSocketFrame
import ru.wdevs.cc1503.Requests._
import ru.wdevs.cc1503.Responses._
import sttp.tapir.{Codec, CodecFormat, DecodeResult}
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import ru.wdevs.cc1503.utils.websocket.PipeParser
import sttp.capabilities

import scala.concurrent.duration._

trait WebsocketClient[F[_]] {
  def run: F[List[MessagingResponseDTO]]
}

class WebsocketClientImpl[F[_]: Async](
  evs: List[WebsocketClient.ClientEvent],
  readDuration: FiniteDuration = 10.seconds,
  readData: Ref[F, List[MessagingResponseDTO]]
)(implicit ws: Fs2Streams[F]) extends WebsocketClient[F] {

  override def run: F[List[MessagingResponseDTO]] = {
    val wsReqStream = evs.traverse {
      case Wait(duration) => Pull.eval(Async[F].sleep(duration))
      case SendMessage(msg) => Pull.output1(msg)
    }.void.stream
    val wsStream = PipeParser.parsePipe[F, Responses.MessagingResponseDTO, Requests.MessagingRequestDTO](
      i => wsReqStream.merge(
        i
          .timeout(readDuration)
          .handleErrorWith(_ => Stream.empty)
          .flatMap(e => Pull.eval(readData.update(_.appended(e))).stream)
      )
    )
    AsyncHttpClientFs2Backend
      .resource[F]()
      .use { backend => {
        basicRequest
          .response(asWebSocketStream(Fs2Streams[F])(wsStream))
          .get(uri"ws://127.0.0.1:8080/messaging")
          .send(backend)
      }.flatMap(_ => readData.get)
      }
  }
}

object WebsocketClient {
  sealed trait ClientEvent
  case class Wait(duration: FiniteDuration) extends ClientEvent
  case class SendMessage(msg: Requests.MessagingRequestDTO) extends ClientEvent

  def make[F[_]: Async](evs: List[WebsocketClient.ClientEvent],
                        readDuration: FiniteDuration = 5.seconds)(implicit ws: Fs2Streams[F]): F[WebsocketClientImpl[F]] = {
    for {
      readData <- Ref.of[F, List[MessagingResponseDTO]](List.empty)
    } yield new WebsocketClientImpl(evs, readDuration, readData)
  }
}
