package ru.wdevs.cc1503

import cats.effect.kernel.Sync
import cats.effect.{Async, IO}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.AnnounceReceiver
import ru.wdevs.cc1503.components.WSRoutesComponent
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

class HttpServer[F[_]: Async: Logger](wsComp: WSRoutesComponent[F], httpRoutes: List[(String, HttpRoutes[F])], config: AppConfig) extends Http4sDsl[F] {


  def start: F[Unit] = {
    for {
      port <- Sync[F].fromOption(
        config.nodes.get(config.id).map(_.port),
        new RuntimeException("Failed to find node port")
      )
      _ <- Logger[F].info("HTTP Server is starting...")
      _ <- BlazeServerBuilder[F]
        .bindHttp(port, "0.0.0.0")
        .withHttpWebSocketApp(wsb => Router(
          {
            ("/ws" -> wsComp.routes(wsb)) ::
              httpRoutes
          }: _*
        ).orNotFound
        )
        .serve
        .drain
        .compile
        .drain
    } yield ()
  }

}
