package ru.wdevs.cc1503.anouncements
import cats.effect.kernel.{Async, Concurrent, GenConcurrent, Sync}
import cats.effect.std.Queue
import fs2.{Chunk, Pull, Stream}
import ru.wdevs.cc1503.anouncements.AnnounceManager.AnnounceMessage
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._

import scala.concurrent.duration._
import scala.collection.mutable

class LocalMessageAnnouncer[F[_]: Async](receiver: AnnounceReceiver[F]) {

  def makeAnnounce(chatId: Channel.Id, text: String, author: String): F[Unit] =
    receiver.receiveAnnounce(chatId, text, author)
}