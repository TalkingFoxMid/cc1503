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
import ru.wdevs.cc1503.anouncements.{AnnounceArbitrator, AnnounceReceiver, GRPCMessageAnnouncer, HttpMessageAnnouncer, LocalMessageAnnouncer}
import ru.wdevs.cc1503.chats.ChatSubscribersRepositoryRedis
import ru.wdevs.cc1503.infra.config.ConfigLoaderImpl
import cats.syntax.all._
import org.http4s.ember.client.EmberClientBuilder
import ru.wdevs.cc1503.components.{RedisComponent, WSRoutesComponent}
import ru.wdevs.cc1503.handling.announce.{GrpcAnnounceHandler, HttpAnnounceHandler}

import java.util.logging.Level
import scala.concurrent.ExecutionContext.global
object MainServ extends IOApp {
  override def run(args: List[String]): IO[effect.ExitCode] = {
    for {
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO].toResource
      RedisComponent(subscribers, matcher) <- RedisComponent.make[IO]
      cfg <- (new ConfigLoaderImpl[IO]).loadConfig.toResource
      emberClient <-  EmberClientBuilder.default[IO].build
      messageReceiver <- AnnounceReceiver.make[IO].toResource

      announceArbitrator = {
        val httpAnnouncer = new HttpMessageAnnouncer(emberClient)
        val grpcAnnouncer = new GRPCMessageAnnouncer[IO]
        val local = new LocalMessageAnnouncer[IO](messageReceiver)
        new AnnounceArbitrator[IO](httpAnnouncer, grpcAnnouncer, local, subscribers, cfg, matcher)
      }
      ms <- MessageStoreLocalImpl.mk[IO].toResource

      httpRun = {
        val wsComp = WSRoutesComponent.mkAsync[IO](ms, messageReceiver, announceArbitrator, subscribers)
        val handlers = (new HttpAnnounceHandler[IO](messageReceiver) :: Nil).map(_.routes)
        val httpServ = new HttpServer[IO](wsComp, handlers, cfg)
        httpServ.start.toResource
      }.start

      grpcRun = {
        val handlers = (new GrpcAnnounceHandler[IO](messageReceiver)) :: Nil
        val grpcServ = new GrpcServer[IO](handlers)
        grpcServ.start
      }

      _ <- (httpRun :: grpcRun :: Nil).parSequence
    } yield ExitCode.Success
  }.useForever
}
