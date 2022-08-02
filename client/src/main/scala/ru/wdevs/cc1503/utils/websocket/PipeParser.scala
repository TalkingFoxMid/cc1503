package ru.wdevs.cc1503.utils.websocket

import fs2.{Pipe, Stream}
import cats.effect.IO
import io.circe.{Decoder, Encoder}
import sttp.ws.WebSocketFrame
import io.circe.syntax._
import ru.wdevs.cc1503.Responses.{MessagingResponseDTO, en}
import io.circe._, io.circe.parser._

object PipeParser {
  def parsePipe[F[_], In: Decoder, Out: Encoder](pipe: Pipe[F, In, Out]): Pipe[F, WebSocketFrame.Data[_], WebSocketFrame] = {
    pipe
      .compose[Stream[F, WebSocketFrame.Data[_]]](
        _.map {
          case WebSocketFrame.Text(p, _, _) =>
            parse(p).flatMap(Decoder[In].decodeJson)
            .getOrElse(throw new NullPointerException())
        })
      .andThen(
        _.map(
          v => WebSocketFrame.text(Encoder[Out].apply(v).noSpaces)
        )
      )
  }

}
