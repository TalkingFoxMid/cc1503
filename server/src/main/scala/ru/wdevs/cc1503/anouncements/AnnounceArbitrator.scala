package ru.wdevs.cc1503.anouncements
import cats.Parallel
import cats.data.OptionT
import cats.effect.kernel.Concurrent
import cats.effect.std.Queue
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._
import ru.wdevs.cc1503.anouncements.AnnounceManager.AnnounceMessage
import fs2.{Pull, Stream}
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.detecting.NodeUserMatcher
import ru.wdevs.cc1503.domain.Nodes.Node
import ru.wdevs.cc1503.infra.config.AppConfig.{AnnounceConfig, AppConfig}

class AnnounceArbitrator[F[_]: Concurrent: Parallel: Logger](
    http: HttpMessageAnnouncer[F],
    grpc: GRPCMessageAnnouncer[F],
    local: LocalMessageAnnouncer[F],
    subscribersRepository: ChatSubscribersRepository[F],
    cfg: AppConfig,
    nodeUserMatcher: NodeUserMatcher[F]
) extends AnnounceManager[F] {
  private def findTargetNodes(chatId: Channel.Id): F[List[Node]] =
    for {
       chatUsers <- subscribersRepository.chatSubscribers(chatId)
       mapping <- nodeUserMatcher.nodesByUsers(chatUsers.toSet)
       uniqueNodes = mapping.values.toSet
      (notFoundIds, foundNodes) = uniqueNodes.toList.map(
        id => cfg.nodes.get(id).map(address => Node(id, address))
          .toRight(id)
      ).separate
    } yield foundNodes

  private def announceTarget(chatId: Channel.Id, text: String): F[Unit] =
    for {
      targetNodesOpt <- findTargetNodes(chatId).map(_.toNel)
      _ <- targetNodesOpt.traverse(
        targetNodes =>
          for {
            _ <- http.makeTargetAnnounce(chatId, text, targetNodes)
              .whenA(cfg.announce.announceViaHttp)
            _ <- grpc.makeTargetAnnounce(chatId, text, targetNodes)
              .whenA(cfg.announce.announceViaGrpc)
          } yield ()
      )
    } yield ()

  override def makeAnnounce(chatId: Channel.Id, text: String, author: String): F[Unit] =
    for {
      _ <- local.makeAnnounce(chatId, text, author)
      _ <- announceTarget(chatId, text)
        .whenA(cfg.announce.announceViaGrpc || cfg.announce.announceViaHttp)
    } yield ()
}
