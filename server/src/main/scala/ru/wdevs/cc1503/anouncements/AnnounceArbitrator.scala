package ru.wdevs.cc1503.anouncements
import cats.Parallel
import cats.effect.kernel.Concurrent
import cats.effect.std.Queue
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage
import fs2.{Pull, Stream}
import org.typelevel.log4cats.Logger

class AnnounceArbitrator[F[_]: Concurrent: Parallel: Logger](
    http: HttpMessageAnnouncer[F],
    grpc: GRPCMessageAnnouncer[F],
) extends MessageAnnouncer[F] {


  private val announcers: List[MessageAnnouncer[F]] = http :: grpc :: Nil

  override def makeAnnounce(chatId: Channel.Id, text: String): F[Unit] =
    announcers.parTraverse(_.makeAnnounce(chatId, text)).void

}
