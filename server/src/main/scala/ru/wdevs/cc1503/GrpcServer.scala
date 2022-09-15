package ru.wdevs.cc1503

import announcement._
import cats.effect.implicits.effectResourceOps
import cats.effect.kernel.{Async, Sync}
import cats.effect.{IO, Resource}
import io.grpc.{ManagedChannelBuilder, Metadata, Server, ServerServiceDefinition}
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import fs2.grpc.syntax.all._
import ru.wdevs.cc1503.anouncements.AnnounceReceiver
import ru.wdevs.cc1503.domain.Channels.Channel

import scala.jdk.CollectionConverters._
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.handling.GrpcHandler
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

class GrpcServer[F[_]: Async: Logger](handlers: List[GrpcHandler[F]], config: AppConfig) {


  private def run: Resource[F, Server] =
    for {
      port <- Sync[F].fromOption(
        config.nodes.get(config.id).map(_.grpcPort),
        new RuntimeException("Failed to find node port")
      ).toResource
      defs <- handlers.traverse(_.srvcDef)
      server <- NettyServerBuilder
        .forPort(port)
        .addServices(defs.asJava)
        .resource[F]
        .evalMap(server => Sync[F].delay(server.start()))
    } yield server



  def start: Resource[F, Unit] =
    for {
      _ <- Logger[F].info("GRPC Server is starting...").toResource
      _ <- run
    } yield ()
}
