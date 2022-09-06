package ru.wdevs.cc1503.anouncements

import cats.data.NonEmptyList
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.domain.Nodes.{Node, NodeAddress}
import cats.syntax.all._

trait TargetMessageAnnouncer[F[_]] {
  def makeTargetAnnounce(chatId: Channel.Id, text: String, nodes: NonEmptyList[Node]): F[Unit]
}
