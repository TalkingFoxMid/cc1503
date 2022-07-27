package ru.wdevs.cc1503.domain

import java.util.UUID

object Channels {
  case class Channel()
  case object Channel {
    case class Id(id: String)
  }
}
