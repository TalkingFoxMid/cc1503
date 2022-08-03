package ru.wdevs.cc1503


import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.unsafe.IORuntime
import fs2.{Pipe, Stream}
import sttp.capabilities.fs2._
import sttp.client3._
import sttp.client3.asynchttpclient.fs2.AsyncHttpClientFs2Backend
import sttp.ws.WebSocketFrame
import ru.wdevs.cc1503.Requests._
import ru.wdevs.cc1503.Responses._
import sttp.tapir.{Codec, CodecFormat, DecodeResult}
import sttp.tapir.json.circe._
import sttp.tapir.generic.auto._
import io.circe.generic.auto._
import ru.wdevs.cc1503.WebsocketClient.SendMessage
import ru.wdevs.cc1503.utils.websocket.PipeParser

object WebSocketStreamFs2 extends IOApp {
  implicit val fsW = Fs2Streams[IO]

  override def run(args: List[String]): IO[ExitCode] = ???
}