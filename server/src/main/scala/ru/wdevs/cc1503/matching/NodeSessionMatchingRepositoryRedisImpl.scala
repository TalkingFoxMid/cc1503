package ru.wdevs.cc1503.matching

class NodeSessionMatchingRepositoryRedisImpl[F[_]] extends NodeSessionMatchingRepository[F] {
  override def rematchToNode(userId: String, nodeId: Int): F[Unit] = ???
}
