package ru.wdevs.cc1503.anouncements
import cats.effect.kernel.{Async, Concurrent, GenConcurrent, Sync}
import cats.effect.std.Queue
import fs2.{Chunk, Pull, Stream}
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._

import scala.concurrent.duration._
import scala.collection.mutable

class LocalMessageAnnouncer[F[_]: Async](queue: Queue[F, MessageAnnouncer.AnnounceMessage]) extends MessageAnnouncer[F] {

  private val eventsStream = Pull.loop[F, MessageAnnouncer.AnnounceMessage, Unit](
    _ => Stream.eval(queue.take).pull.echo.map(_.some)
  ).apply(()).stream


  override def announce(chatId: Channel.Id, text: String): F[Unit] =
    queue.offer(AnnounceMessage(chatId))

  override def subscribe(chatId: Channel.Id): fs2.Stream[F, MessageAnnouncer.AnnounceMessage] =
    eventsStream.filter(_.chatId == chatId)

}

object LocalMessageAnnouncer {
  def make[F[_]: Async]: F[LocalMessageAnnouncer[F]] =
    for {
      q <- Queue.unbounded[F, MessageAnnouncer.AnnounceMessage]
    } yield new LocalMessageAnnouncer[F](q)
}