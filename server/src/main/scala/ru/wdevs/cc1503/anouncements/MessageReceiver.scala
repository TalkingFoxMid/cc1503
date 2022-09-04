package ru.wdevs.cc1503.anouncements

import ru.wdevs.cc1503.domain.Channels.Channel

trait MessageReceiver[F[_]] {
  def receiveMessage(chatId: Channel.Id, text: String): F[Unit]
}
