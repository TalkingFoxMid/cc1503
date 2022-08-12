package ru.wdevs.cc1503.chats

import ru.wdevs.cc1503.domain.Channels.Channel

trait ChatSubscribersRepository[F[_]] {
  def chatSubscribers(chat: Channel.Id): F[List[String]]

  def userChats(userId: String): F[List[Channel.Id]]

  def subscribeChat(chat: Channel.Id, userId: String): F[Unit]

  def leaveChat(chat: Channel.Id, userId: String): F[Unit]
}
