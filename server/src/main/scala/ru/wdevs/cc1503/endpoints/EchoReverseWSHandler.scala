package ru.wdevs.cc1503.endpoints

import cats.effect.Async
import cats.effect.kernel.Sync
import sttp.tapir.{CodecFormat, PublicEndpoint, endpoint, webSocketBody}
import fs2.Pipe
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.{CodecFormat, PublicEndpoint}

class EchoReverseWSHandler[F[_]: Async] extends WSHandler[F, String, String] {
  val wsEndpoint: PublicEndpoint[Unit, Unit, Pipe[F, String, String], Fs2Streams[F] with WebSockets] =
    endpoint.get.in("multiply").out(webSocketBody[String, CodecFormat.TextPlain, String, CodecFormat.TextPlain](Fs2Streams[F]))
  override val sync: Sync[F] = Sync[F]
  override val pipe: Pipe[F, String, String] = _.map(_.reverse)
}
