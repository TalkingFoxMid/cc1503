package ru.wdevs.cc1503

import cats.effect
import cats.effect.{ExitCode, IO, IOApp, Ref}
import org.slf4j.{Logger, LoggerFactory}
import org.typelevel.log4cats.LoggerName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import ru.wdevs.cc1503.domain.Channels.Channel
import ru.wdevs.cc1503.storing.MessageStore.StoredMessage
import ru.wdevs.cc1503.storing.{MessageStore, MessageStoreLocalImpl}
import org.typelevel.log4cats.slf4j._

import java.util.logging.Level
object Main extends IOApp {
  override def run(args: List[String]): IO[effect.ExitCode] = {
    for {
      ref <- Ref[IO].of[Map[Channel.Id, List[StoredMessage]]](
        Map.empty
      )
      implicit0(name: LoggerName) = LoggerName(org.slf4j.Logger.ROOT_LOGGER_NAME)
      _ = println(name)
            _ = print(LoggerName.name)

      log= LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)
      _= println(log.isInfoEnabled)

      logger <- Slf4jLogger.fromSlf4j[IO](LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME))
      _ <- logger.isInfoEnabled.map(println)
      _ <- logger.info("qweqweqweqwe")
      server = new Server[IO]
      ms: MessageStore[IO] = new MessageStoreLocalImpl[IO](ref)
      wsComponent = WSRoutesComponent.mkAsync[IO](ms)
      _ <- server.start(wsComponent)

    } yield ExitCode.Success
  }
}
