package ru.wdevs.cc1503.anouncements
import cats.Monad
import cats.effect.kernel.{Async, Sync}
import cats.effect.std.Queue
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig._
import cats.syntax.all._
import fs2.{Pull, Stream}
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

class HttpMessageAnnouncer[F[_]: Monad](
  subscribers: ChatSubscribersRepository[F],
  config: AppConfig,
  queue: Queue[F, MessageAnnouncer.AnnounceMessage]
) extends MessageAnnouncer[F] with MessageReceiver[F] {

  private val eventsStream = Pull.loop[F, MessageAnnouncer.AnnounceMessage, Unit](
    _ => Stream.eval(queue.take).pull.echo.map(_.some)
  ).apply(()).stream

  override def announce(chatId: Channel.Id, text: String): F[Unit] =
    Monad[F].unit

  override def subscribe(chatIds: List[Channel.Id]): fs2.Stream[F, MessageAnnouncer.AnnounceMessage] =
    eventsStream.filter(ev => chatIds.contains(ev.chatId))

  override def receiveMessage(chatId: Channel.Id, text: String): F[Unit] =
    queue.offer(AnnounceMessage(chatId, text))
}

object HttpMessageAnnouncer {
  def make[F[_]: Async](subscribers: ChatSubscribersRepository[F],
                        config: AppConfig): F[HttpMessageAnnouncer[F]] =
    for {
      q <- Queue.unbounded[F, MessageAnnouncer.AnnounceMessage]
    } yield new HttpMessageAnnouncer[F](subscribers, config, q)
}