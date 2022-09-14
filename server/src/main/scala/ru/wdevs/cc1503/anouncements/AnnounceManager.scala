package ru.wdevs.cc1503.anouncements

import ru.wdevs.cc1503.domain.Channels.Channel
import fs2.Stream
import ru.wdevs.cc1503.anouncements.AnnounceManager.AnnounceMessage

trait AnnounceManager[F[_]] {
  def makeAnnounce(chatId: Channel.Id, text: String, author: String): F[Unit]
}

object AnnounceManager {
  case class AnnounceMessage(chatId: Channel.Id, text: String, author: String)
}