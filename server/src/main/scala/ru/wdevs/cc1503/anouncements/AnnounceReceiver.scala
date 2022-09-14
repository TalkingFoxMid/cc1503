package ru.wdevs.cc1503.anouncements

import cats.{Monad, Parallel}
import cats.effect.{ExitCode, IO, IOApp}
import cats.effect.kernel.Async
import cats.effect.std.Queue
import cats.syntax.all._
import fs2.Pull
import fs2.concurrent.Topic
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.AnnounceManager.AnnounceMessage
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

import scala.concurrent.duration._

trait AnnounceReceiver[F[_]] {
  def receiveAnnounce(chatId: Channel.Id, text: String, author: String): F[Unit]

  def subscribeToAnnounces(
      chatIds: List[Channel.Id]
  ): fs2.Stream[F, AnnounceManager.AnnounceMessage]
}

class AnnounceReceiverImpl[F[_]: Logger: Monad](
    topic: Topic[F, AnnounceManager.AnnounceMessage]
) extends AnnounceReceiver[F] {



  override def subscribeToAnnounces(
      chatIds: List[Channel.Id]
  ): fs2.Stream[F, AnnounceManager.AnnounceMessage] =
    topic.subscribe(100).filter(ev => chatIds.contains(ev.chatId))

  override def receiveAnnounce(chatId: Channel.Id, text: String, author: String): F[Unit] =
    Logger[F].info(s"GRPC: Received message from ${chatId.id}") *> topic.publish1(
      AnnounceMessage(chatId, text, author)
    ).void
}

object AnnounceReceiver {
  def make[F[_]: Async: Logger]: F[AnnounceReceiver[F]] =
    for {
      topic <- Topic[F, AnnounceManager.AnnounceMessage]
    } yield new AnnounceReceiverImpl[F](topic)
}


object Testing extends IOApp {
  import fs2.Stream

  override def run(args: List[String]): IO[ExitCode] =
    for {
      queue <- Queue.unbounded[IO, Int]
      eventsStream = Pull
        .loop[IO, Int, Unit](_ =>
          for {
            el <- Pull.eval(queue.take)
            _ <- Pull.output1(el)
          } yield Some(())
        )
        .apply(())
        .stream

      _ <- eventsStream.map(println _)
        .compile.drain.start
      _ <- eventsStream.map(println _)
        .compile.drain.start

      _ <- List.range(0, 50).traverse(
        d => for {
          _ <- IO.sleep(3.seconds)
          _ <- queue.offer(d)
        } yield ()
      )

    } yield ExitCode.Success
}