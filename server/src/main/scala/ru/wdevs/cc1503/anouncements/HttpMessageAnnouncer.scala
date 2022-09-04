package ru.wdevs.cc1503.anouncements
import cats.Monad
import cats.effect.IO
import cats.effect.kernel.{Async, Sync}
import cats.effect.std.Queue
import cats.effect.unsafe.IORuntime
import ru.wdevs.cc1503.chats.ChatSubscribersRepository
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.infra.config.AppConfig._
import cats.syntax.all._
import fs2.{Pull, Stream}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig
import sttp.client3._

import scala.concurrent.ExecutionContext.global

class HttpMessageAnnouncer[F[_]: Sync: Logger](
    subscribers: ChatSubscribersRepository[F],
    config: AppConfig,
    client: Client[F],
    queue: Queue[F, MessageAnnouncer.AnnounceMessage]
) extends MessageAnnouncer[F]
    with MessageReceiver[F] {

  private val eventsStream = Pull
    .loop[F, MessageAnnouncer.AnnounceMessage, Unit](_ =>
      for {
        el <- Pull.eval(queue.take)
        _ <- Pull.eval(Logger[F].info("FOUND MESSAGE AT QUEUE"))
        _ <- Pull.output1(el)
      } yield Some(())
    )
    .apply(())
    .stream

  private def sendToNode(host: String, chatId: Channel.Id, text: String): F[Unit] =
    for {
      _ <- Logger[F].info(s"Sending msg to http://${host}/announce/hello/${chatId.id}/${text}")
      _ <- client.get(s"http://${host}/announce/hello/${chatId.id}/$text")(Sync[F].pure)
    } yield ()

  override def announce(chatId: Channel.Id, text: String): F[Unit] =
    config.nodes.toList.traverse {
      case (id, ip) if id != config.id => sendToNode(ip, chatId, text)
      case _ => Monad[F].unit
    }.void

  override def subscribe(
      chatIds: List[Channel.Id]
  ): fs2.Stream[F, MessageAnnouncer.AnnounceMessage] =
    eventsStream.filter(ev => chatIds.contains(ev.chatId))

  override def receiveMessage(chatId: Channel.Id, text: String): F[Unit] =
    Logger[F].info(s"Received message from ${chatId.id}") *> queue.offer(
      AnnounceMessage(chatId, text)
    )

}

object HttpMessageAnnouncer {
  def make[F[_]: Async: Logger](
      subscribers: ChatSubscribersRepository[F],
      config: AppConfig,
      client: Client[F]
  ): F[HttpMessageAnnouncer[F]] =
    for {
      q <- Queue.unbounded[F, MessageAnnouncer.AnnounceMessage]
    } yield new HttpMessageAnnouncer[F](subscribers, config, client, q)
}

object TEST extends App {
  import scala.concurrent.duration._

  val f = for {
    q <- Queue.unbounded[IO, Int]
    eventsStream = Pull
      .loop[IO, Int, Unit](_ =>
        for {
          el <- Pull.eval(q.take)
          _ <- Pull.output1(el)
        } yield Some(())
      )
      .apply(())
      .stream
    _ <- q.offer(10)
    z <- List.range(0, 100)
      .traverse(
        el => q.offer(el) *> IO.sleep(5.seconds)
      ).start
    _ <- eventsStream
      .flatMap(
        el => Stream.eval[IO, Unit](IO(println(el)))
      ).compile.drain
  } yield ()
  f.unsafeRunSync()(IORuntime.global)
}