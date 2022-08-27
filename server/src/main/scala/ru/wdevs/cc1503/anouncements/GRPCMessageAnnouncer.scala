package ru.wdevs.cc1503.anouncements
import cats.Monad
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._

class GRPCMessageAnnouncer[F[_]: Monad](subscribers: ChatSubscribersRepository[F]) extends MessageAnnouncer[F] {
  override def announce(chatId: Channel.Id, text: String): F[Unit] =
    for {
      subs <- subscribers.chatSubscribers(chatId)
    } yield ()

  override def subscribe(chatIds: List[Channel.Id]): fs2.Stream[F, MessageAnnouncer.AnnounceMessage] = ???
}