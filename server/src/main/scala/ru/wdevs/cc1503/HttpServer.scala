package ru.wdevs.cc1503

import cats.effect.kernel.Sync
import cats.effect.{Async, IO}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import cats.syntax.all._
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.endpoints.http.MsgAnnounceHttpEndpoint
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

class HttpServer[F[_]: Async: Logger] {
  def start(wsComp: WSRoutesComponent[F], announceEndpoint: MsgAnnounceHttpEndpoint[F], config: AppConfig): F[Unit] = {
    for {
      port <- Sync[F].fromOption(
        config.nodes.get(config.id).flatMap(_.split(":").last.toIntOption),
        new RuntimeException("Failed to find node port")
      )
      _ <- Logger[F].info("Server is starting...")
      _ <- BlazeServerBuilder[F]
        .bindHttp(port, "localhost")
        .withHttpWebSocketApp(wsb => Router(
          "/ws" -> wsComp.routes(wsb),
          "/announce" -> announceEndpoint.helloWorldService).orNotFound
        )
        .serve
        .drain
        .compile
        .drain
    } yield ()
  }

}
