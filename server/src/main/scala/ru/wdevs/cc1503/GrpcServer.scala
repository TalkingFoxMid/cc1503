package ru.wdevs.cc1503

import announcement._
import cats.effect.kernel.Async
import cats.effect.{IO, Resource}
import io.grpc.{ManagedChannelBuilder, Metadata, ServerServiceDefinition}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._
import ru.wdevs.cc1503.anouncements.AnnounceReceiver
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._

class GrpcServer[F[_]: Async](messageReceiver: AnnounceReceiver[F]) {
//
  val announcementService = AnnouncementServiceFs2Grpc
    .bindServiceResource[F](
      (message, _) => messageReceiver.receiveAnnounce(Channel.Id(message.chatId), message.text)
        .as(AnResponse(0))
    )

  def run(service: ServerServiceDefinition) = NettyServerBuilder
    .forPort(8091)
    .addService(service)
    .resource[IO]
    .evalMap(server => IO(server.start()))
    .useForever
}
