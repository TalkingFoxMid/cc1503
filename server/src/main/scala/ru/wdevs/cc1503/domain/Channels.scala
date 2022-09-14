package ru.wdevs.cc1503.domain

import cats.Eq

import java.util.UUID

object Channels {
  case class Channel()
  case object Channel {
    case class Id(id: String)

    object Id {
      implicit val channelIdEq = Eq.fromUniversalEquals[Id]
    }
  }
}
