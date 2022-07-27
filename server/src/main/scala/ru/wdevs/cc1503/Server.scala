package ru.wdevs.cc1503

import cats.effect.Async
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import cats.syntax.all._
import org.typelevel.log4cats.Logger

class Server[F[_]: Async] {
  def start(wsComp: WSRoutesComponent[F]): F[Unit] = for {
//    _ <- Logger[F].info("Server is starting...")
    _ <- BlazeServerBuilder[F]
      .bindHttp(8080, "localhost")
      .withHttpWebSocketApp(wsb => Router("/" -> wsComp.routes(wsb)).orNotFound)
      .serve
      .drain
      .compile
      .drain
  } yield ()

}
