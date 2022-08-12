package ru.wdevs.cc1503.anouncements
import ru.wdevs.cc1503.domain.Channels.Channel

class GRPCMessageAnnouncer[F[_]] extends MessageAnnouncer[F] {
  override def announce(chatId: Channel.Id, text: String): F[Unit] = ???

  override def subscribe(chatIds: List[Channel.Id]): fs2.Stream[F, MessageAnnouncer.AnnounceMessage] = ???
}
