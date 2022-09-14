package ru.wdevs.cc1503.handling.announce

import cats.{Applicative, Monad}
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import ru.wdevs.cc1503.anouncements.AnnounceReceiver
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.handling.HttpHandler

class HttpAnnounceHandler[F[_]: Monad](receiver: AnnounceReceiver[F]) extends HttpHandler[F] with Http4sDsl[F] {
  private val announceService = HttpRoutes.of[F] {
    case GET -> Root / chatId / text / author =>
      for {
        _ <- receiver.receiveAnnounce(Channel.Id(chatId), text, author)
        res <- Ok(s"send")
      } yield res
  }

  override def routes: (String, HttpRoutes[F]) = "/announce" -> announceService
}
