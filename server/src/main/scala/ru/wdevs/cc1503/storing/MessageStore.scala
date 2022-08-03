package ru.wdevs.cc1503.storing

import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.domain.Messaging.Message
import ru.wdevs.cc1503.storing.MessageStore.StoredMessage

trait MessageStore[F[_]] {
  def saveMessage(text: String, channelId: Channel.Id, author: String): F[Unit]

  def getMessages(chId: Channel.Id, limit: Int): F[List[StoredMessage]]
}

object MessageStore {
  case class StoredMessage(text: String, author: String)
}
