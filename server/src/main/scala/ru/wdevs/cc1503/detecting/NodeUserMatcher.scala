package ru.wdevs.cc1503.detecting

import cats.{Applicative, Functor, Monad}
import dev.profunktor.redis4cats.RedisCommands
import cats.syntax.all._
import org.typelevel.log4cats.Logger

trait NodeUserMatcher[F[_]] {
  def nodesByUsers(userIds: Set[String]): F[Map[String, String]]

  def matchUserToNode(userId: String, nodeId: String): F[Unit]
}

class NodeUserMatcherRedisImpl[F[_]: Monad: Logger](redis: RedisCommands[F, String, String]) extends NodeUserMatcher[F] {
  override def nodesByUsers(userIds: Set[String]): F[Map[String, String]] =
    for {
      kToNodes <- redis.mGet(userIds.map(userId => s"sessionBinding:$userId"))
      (failed, ok) = kToNodes.map {
        case (s"sessionBinding:$userId", str1) => Right((userId, str1))
        case (k, _) => Left(k)
      }.toList.separate
      _ <- failed.traverse(fkey => Logger[F].warn(s"Failed to find node mapping for key $fkey"))
    } yield ok.toMap

  override def matchUserToNode(userId: String, nodeId: String): F[Unit] =
    redis.set(s"sessionBinding:$userId", nodeId)
}