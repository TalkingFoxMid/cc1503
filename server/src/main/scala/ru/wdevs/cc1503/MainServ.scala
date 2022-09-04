package ru.wdevs.cc1503

import cats.effect
import cats.effect.implicits.effectResourceOps
import cats.effect.{ExitCode, IO, IOApp, Ref, Resource}
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
import org.http4s.ember.client.EmberClientBuilder

import java.util.logging.Level
import scala.concurrent.ExecutionContext.global
object MainServ extends IOApp {
  override def run(args: List[String]): IO[effect.ExitCode] = {
    for {
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO].toResource
      subscribers <- ChatSubscribersRepositoryRedis.mkAsync[IO]
      server = new HttpServer[IO]
      cfg <- (new ConfigLoaderImpl[IO]).loadConfig.toResource
      emberClient <-  EmberClientBuilder.default[IO].build
      lma <- HttpMessageAnnouncer.make[IO](subscribers, cfg, emberClient).toResource
      ms <- MessageStoreLocalImpl.mk[IO].toResource
      ws = WSRoutesComponent.mkAsync[IO](ms, lma, subscribers)
      _ <- server.start(ws, new MsgAnnounceHttpEndpoint[IO](lma), cfg).toResource
    } yield ExitCode.Success
  }.useForever
}
