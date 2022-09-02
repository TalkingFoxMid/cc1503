package ru.wdevs.cc1503.anouncements
import cats.Monad
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig._
import cats.syntax.all._

class HttpMessageAnnouncer[F[_]: Monad](subscribers: ChatSubscribersRepository[F], config: AppConfig) extends MessageAnnouncer[F] {
  override def announce(chatId: Channel.Id, text: String): F[Unit] =
    ???

  override def subscribe(chatIds: List[Channel.Id]): fs2.Stream[F, MessageAnnouncer.AnnounceMessage] = ???
}