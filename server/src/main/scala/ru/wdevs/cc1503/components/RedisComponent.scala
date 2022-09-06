package ru.wdevs.cc1503.components

import cats.effect.Resource
import cats.effect.kernel.Async
import dev.profunktor.redis4cats.Redis
import ru.wdevs.cc1503.chats.{ChatSubscribersRepository, ChatSubscribersRepositoryRedis}
import dev.profunktor.redis4cats.effect.Log.Stdout._
import org.typelevel.log4cats.Logger
import ru.wdevs.cc1503.detecting.{NodeUserMatcher, NodeUserMatcherRedisImpl}

case class RedisComponent[F[_]](
  subscribers: ChatSubscribersRepository[F],
  matcher: NodeUserMatcher[F]
)

object RedisComponent {
  def make[F[_]: Async: Logger]: Resource[F, RedisComponent[F]] =
    Redis[F].utf8("redis://localhost").map(
      redisCommands =>
        RedisComponent(
          new ChatSubscribersRepositoryRedis(redisCommands),
          new NodeUserMatcherRedisImpl[F](redisCommands)
        )
    )
}
