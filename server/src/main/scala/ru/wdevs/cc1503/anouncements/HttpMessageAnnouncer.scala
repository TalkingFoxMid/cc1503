package ru.wdevs.cc1503.anouncements
import cats.Monad
import cats.data.NonEmptyList
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
import ru.wdevs.cc1503.anouncements.AnnounceManager.AnnounceMessage
import ru.wdevs.cc1503.domain.Nodes.{Node, NodeAddress}
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig
import sttp.client3._

import scala.concurrent.ExecutionContext.global

class HttpMessageAnnouncer[F[_]: Sync: Logger](
    client: Client[F]
) {

  private def sendToNode(address: NodeAddress, chatId: Channel.Id, text: String): F[Unit] =
    for {
      _ <- client.get(s"http://${address.host}:${address.port}/announce/hello/${chatId.id}/$text")(Sync[F].pure)
    } yield ()

  def makeTargetAnnounce(chatId: Channel.Id, text: String, nodes: NonEmptyList[Node]): F[Unit] =
    nodes.traverse(node => sendToNode(node.address, chatId, text)).void
}

