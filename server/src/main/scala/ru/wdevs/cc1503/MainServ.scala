package ru.wdevs.cc1503

import cats.effect
import cats.effect.{ExitCode, IO, IOApp, Ref}
import org.typelevel.log4cats.{Logger, LoggerName}
import org.typelevel.log4cats.slf4j.Slf4jLogger
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.storing.MessageStore.StoredMessage
import ru.wdevs.cc1503.storing.{MessageStore, MessageStoreLocalImpl}
import org.typelevel.log4cats.slf4j._
import ru.wdevs.cc1503.anouncements.{HttpMessageAnnouncer, LocalMessageAnnouncer}
import ru.wdevs.cc1503.chats.ChatSubscribersRepositoryRedis
import ru.wdevs.cc1503.endpoints.http.MsgAnnounceHttpEndpoint
import ru.wdevs.cc1503.infra.config.ConfigLoaderImpl
import cats.syntax.all._

import java.util.logging.Level
object MainServ extends IOApp {
  override def run(args: List[String]): IO[effect.ExitCode] = {
    for {
      ref <- Ref[IO].of[Map[Channel.Id, List[StoredMessage]]](
        Map.empty
      )
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
      configLoader = new ConfigLoaderImpl[IO]
      cfg <- configLoader.loadConfig
      ms: MessageStore[IO] = new MessageStoreLocalImpl[IO](ref)
      _ = println(cfg)
      _ <- ChatSubscribersRepositoryRedis.mkAsync[IO].use(
        subscribers => {
          val wsServer = new HttpServer[IO]

          for {
            lma <- HttpMessageAnnouncer.make[IO](subscribers, cfg)
            ws = WSRoutesComponent.mkAsync[IO](ms, lma, subscribers)
            _ <- wsServer.start(ws, new MsgAnnounceHttpEndpoint[IO](lma), cfg)
          } yield ()
        }
      )
    } yield ExitCode.Success
  }
}
