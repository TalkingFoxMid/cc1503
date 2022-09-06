package ru.wdevs.cc1503.anouncements

import announcement.{AnnounceMessage, AnnouncementServiceFs2Grpc}
import cats.Monad
import cats.data.NonEmptyList
import cats.effect.kernel.{Async, Sync}
import cats.effect.std.Queue
import cats.syntax.all._
import fs2.Pull
import fs2.grpc.syntax.all.fs2GrpcSyntaxManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.domain.Nodes.{Node, NodeAddress}
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

class GRPCMessageAnnouncer[F[_]: Async: Logger](
) {
  private def sendToNode(address: NodeAddress, chatId: Channel.Id, text: String): F[Unit] =
    NettyChannelBuilder
      .forAddress(address.host, address.port)
      .resource[F].flatMap(r => AnnouncementServiceFs2Grpc.stubResource[F](r))
      .use(
        _.announce(AnnounceMessage(chatId.id, text), new Metadata).void
      )

  def makeTargetAnnounce(chatId: Channel.Id, text: String, nodes: NonEmptyList[Node]): F[Unit] =
    nodes.traverse(node => sendToNode(node.address, chatId, text)).void
  //    nodeIds.map(id => (id, config.nodes.get(id))).traverse {
//      case (id, address) if id != config.id => sendToNode(address, chatId, text)
//      case _ => Monad[F].unit
//    }.void
}
