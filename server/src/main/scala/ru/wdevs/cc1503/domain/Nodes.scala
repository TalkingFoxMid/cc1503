package ru.wdevs.cc1503.domain

object Nodes {
  case class NodeAddress(host: String, port: Int, grpcPort: Int)
  case class Node(
      id: String,
      address: NodeAddress
  )
}
