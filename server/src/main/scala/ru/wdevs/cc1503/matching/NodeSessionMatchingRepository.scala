package ru.wdevs.cc1503.matching

trait NodeSessionMatchingRepository[F[_]] {
  def rematchToNode(userId: String, nodeId: Int): F[Unit]
}
