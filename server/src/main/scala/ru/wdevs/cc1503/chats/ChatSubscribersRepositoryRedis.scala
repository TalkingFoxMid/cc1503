package ru.wdevs.cc1503.chats
import cats.Monad
import cats.effect.{ExitCode, IO, IOApp}
import ru.wdevs.cc1503.domain.Channels.Channel
import cats.syntax.all._
import dev.profunktor.redis4cats.RedisCommands
import ru.wdevs.cc1503.chats.ChatSubscribersRepositoryRedis._
import cats.effect._
import dev.profunktor.redis4cats.Redis
import dev.profunktor.redis4cats.effect.Log.Stdout._

class ChatSubscribersRepositoryRedis[F[_]: Monad](redis: RedisCommands[F, String, String]) extends ChatSubscribersRepository[F] {

  override def chatSubscribers(chat: Channel.Id): F[List[String]] =
    redis.sMembers(redisChatUsersSet(chat)).map(_.toList)

  override def userChats(userId: String): F[List[Channel.Id]] = {
    redis.sMembers(userChatsSet(userId)).map(_.toList.map(Channel.Id.apply))
  }


  override def subscribeChat(chat: Channel.Id, userId: String): F[Unit] =
    redis.transact_(
      redis.sAdd(redisChatUsersSet(chat), userId).void ::
        redis.sAdd(userChatsSet(userId), chat.id).void ::
        Nil
    )

  override def leaveChat(chat: Channel.Id, userId: String): F[Unit] =
    redis.transact_(
      redis.sRem(redisChatUsersSet(chat), userId) ::
        redis.sRem(userChatsSet(userId), chat.id) ::
        Nil
    )

}

object ChatSubscribersRepositoryRedis {
  def redisChatUsersSet(chat: Channel.Id): String = s"chat_users:${chat.id}"

  def userChatsSet(userId: String): String = s"user_chats:$userId"

  def mkAsync[F[_]: Async]: Resource[F, ChatSubscribersRepositoryRedis[F]] =
    Redis[F].utf8("redis://localhost").map(
      redisCommands => new ChatSubscribersRepositoryRedis(redisCommands)
    )
}

object App extends IOApp {


  override def run(args: List[String]): IO[ExitCode] =
    Redis[IO].utf8("redis://localhost").use { redis =>
      for {
        res <- redis.sMembers("vff")
        _ = println(res)
      } yield ExitCode.Success
    }
}

object A extends App {
  println("x-lang".toLowerCase)
}