package ru.wdevs.cc1503.components

import cats.effect.kernel.Async
import org.http4s.HttpRoutes
import org.http4s.server.websocket.WebSocketBuilder2
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.{AnnounceReceiver, MessageAnnouncer}
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.endpoints.{EchoReverseWSHandler, MessagingWSHandler}
import ru.wdevs.cc1503.storing.MessageStore
import sttp.tapir.server.http4s.Http4sServerInterpreter

trait WSRoutesComponent[F[_]] {
  def routes: WebSocketBuilder2[F] => HttpRoutes[F]
}

object WSRoutesComponent {

  def mkAsync[F[_]: Async: Logger](ms: MessageStore[F], receiver: AnnounceReceiver[F], announcer: MessageAnnouncer[F], subscribers: ChatSubscribersRepository[F]): WSRoutesComponent[F] =
    new WSRoutesComponent[F] {

      override def routes: WebSocketBuilder2[F] => HttpRoutes[F] =
        Http4sServerInterpreter[F]()
          .toWebSocketRoutes(
            List(
              new EchoReverseWSHandler[F]().buildWithLogic,
              new MessagingWSHandler[F](ms, receiver, announcer, subscribers).buildWithLogic
            )
          )
    }
}
