package ru.wdevs.cc1503.handling.announce

import announcement.{AnResponse, AnnouncementServiceFs2Grpc}
import cats.Functor
import cats.effect.kernel.Async
import ru.wdevs.cc1503.domain.Channels.Channel
import io.grpc.{ManagedChannelBuilder, Metadata, Server, ServerServiceDefinition}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._
import ru.wdevs.cc1503.anouncements.AnnounceReceiver
import cats.syntax.all._
import ru.wdevs.cc1503.handling.GrpcHandler

class GrpcAnnounceHandler[F[_]: Async](receiver: AnnounceReceiver[F]) extends GrpcHandler[F] {
  val srvcDef =
    AnnouncementServiceFs2Grpc
      .bindServiceResource[F](
        (message, _) => receiver.receiveAnnounce(Channel.Id(message.chatId), message.text, message.author)
          .as(AnResponse(0))
      )
}
