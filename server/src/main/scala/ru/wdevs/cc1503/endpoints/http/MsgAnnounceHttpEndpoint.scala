package ru.wdevs.cc1503.endpoints.http

import cats.Monad
import cats.effect.IO
import cats.effect._
import org.http4s._
import cats.ApplicativeError
import cats.data.NonEmptyVector
import cats.effect.{Async, Concurrent}
import cats.implicits._
import io.circe._
import io.circe.syntax._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import ru.wdevs.cc1503.anouncements.MessageReceiver
import ru.wdevs.cc1503.domain.Channels.Channel

class MsgAnnounceHttpEndpoint[F[_]: Sync](messageReceiver: MessageReceiver[F]) extends Http4sDsl[F]{
  val helloWorldService = HttpRoutes.of[F] {
    case GET -> Root / "hello" / chatId / text =>
      for {
        _ <- messageReceiver.receiveMessage(Channel.Id(chatId), text)
        res <- Ok(s"send")
      } yield res
  }
}
