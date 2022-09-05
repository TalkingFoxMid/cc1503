package ru.wdevs.cc1503.anouncements
import cats.Monad
import cats.effect.IO
import cats.effect.kernel.{Async, Sync}
import cats.effect.std.Queue
import cats.effect.unsafe.IORuntime
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig._
import cats.syntax.all._
import fs2.{Pull, Stream}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig
import sttp.client3._

import scala.concurrent.ExecutionContext.global

class HttpMessageAnnouncer[F[_]: Sync: Logger](
    subscribers: ChatSubscribersRepository[F],
    config: AppConfig,
    client: Client[F],
) extends MessageAnnouncer[F] {

  private def sendToNode(host: String, chatId: Channel.Id, text: String): F[Unit] =
    for {
      _ <- Logger[F].info(s"Sending msg to http://${host}/announce/hello/${chatId.id}/${text}")
      _ <- client.get(s"http://${host}/announce/hello/${chatId.id}/$text")(Sync[F].pure)
    } yield ()

  override def makeAnnounce(chatId: Channel.Id, text: String): F[Unit] =
    config.nodes.toList.traverse {
      case (id, ip) if id != config.id => sendToNode(ip, chatId, text)
      case _ => Monad[F].unit
    }.void

}

