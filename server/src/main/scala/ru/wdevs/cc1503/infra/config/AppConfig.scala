package ru.wdevs.cc1503.infra.config

import ru.wdevs.cc1503.domain.Nodes.NodeAddress

object AppConfig {
  case class AppConfig(
    nodes: Map[String, NodeAddress],
    id: String,
    announce: AnnounceConfig
  )

  case class AnnounceConfig(
      announceViaHttp: Boolean,
      announceViaGrpc: Boolean
  )


}
