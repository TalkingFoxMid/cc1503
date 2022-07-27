package ru.wdevs.cc1503.domain

import Channels.Channel

import java.util.UUID

object Messaging {
  case class Message(chId: Channel.Id, text: String)
}
