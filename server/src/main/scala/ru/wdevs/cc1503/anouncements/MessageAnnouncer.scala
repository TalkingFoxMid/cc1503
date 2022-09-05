package ru.wdevs.cc1503.anouncements

import ru.wdevs.cc1503.domain.Channels.Channel
import fs2.Stream
import ru.wdevs.cc1503.anouncements.MessageAnnouncer.AnnounceMessage

trait MessageAnnouncer[F[_]] {
  def makeAnnounce(chatId: Channel.Id, text: String): F[Unit]
}

object MessageAnnouncer {
  case class AnnounceMessage(chatId: Channel.Id, text: String)
}