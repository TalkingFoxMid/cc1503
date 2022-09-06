package ru.wdevs.cc1503.anouncements

import cats.Monad
import cats.effect.kernel.Async
import cats.effect.std.Queue
import cats.syntax.all._
import fs2.Pull
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.AnnounceManager.AnnounceMessage
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

trait AnnounceReceiver[F[_]] {
  def receiveAnnounce(chatId: Channel.Id, text: String): F[Unit]

  def subscribeToAnnounces(
      chatIds: List[Channel.Id]
  ): fs2.Stream[F, AnnounceManager.AnnounceMessage]
}

class AnnounceReceiverImpl[F[_]: Logger: Monad](
    queue: Queue[F, AnnounceManager.AnnounceMessage]
) extends AnnounceReceiver[F] {
  private val eventsStream = Pull
    .loop[F, AnnounceManager.AnnounceMessage, Unit](_ =>
      for {
        el <- Pull.eval(queue.take)
        _ <- Pull.eval(Logger[F].info("FOUND MESSAGE AT QUEUE"))
        _ <- Pull.output1(el)
      } yield Some(())
    )
    .apply(())
    .stream

  override def subscribeToAnnounces(
      chatIds: List[Channel.Id]
  ): fs2.Stream[F, AnnounceManager.AnnounceMessage] =
    eventsStream.filter(ev => chatIds.contains(ev.chatId))

  override def receiveAnnounce(chatId: Channel.Id, text: String): F[Unit] =
    Logger[F].info(s"GRPC: Received message from ${chatId.id}") *> queue.offer(
      AnnounceMessage(chatId, text)
    )
}

object AnnounceReceiver {
  def make[F[_]: Async: Logger]: F[AnnounceReceiver[F]] =
    for {
      q <- Queue.unbounded[F, AnnounceManager.AnnounceMessage]
    } yield new AnnounceReceiverImpl[F](q)
}