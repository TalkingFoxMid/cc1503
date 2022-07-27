package ru.wdevs.cc1503


import cats.effect.IO
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

object WebSocketStreamFs2 extends App {
  implicit val runtime: IORuntime = cats.effect.unsafe.implicits.global
  val cd = implicitly[Codec[WebSocketFrame, MessagingRequestDTO, CodecFormat.Json]]

  def webSocketFramePipe: Pipe[IO, WebSocketFrame.Data[_], WebSocketFrame] =
    PipeParser.parsePipe[IO, Responses.MessagingResponseDTO, Requests.MessagingRequestDTO](
      i => Stream.emit(FetchMessagesDTO("omg"))
    )

  AsyncHttpClientFs2Backend
    .resource[IO]()
    .use { backend => {
      basicRequest
        .response(asWebSocketStream(Fs2Streams[IO])(webSocketFramePipe))
        .get(uri"ws://127.0.0.1:8080/messaging")
        .send(backend)
    }
    }
    .unsafeRunSync()
}