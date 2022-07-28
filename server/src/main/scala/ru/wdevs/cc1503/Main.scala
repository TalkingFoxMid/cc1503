package ru.wdevs.cc1503

import cats.effect
import cats.effect.{ExitCode, IO, IOApp, Ref}
import org.typelevel.log4cats.{Logger, LoggerName}
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
      implicit0(logger: Logger[IO]) <- Slf4jLogger.create[IO]
      server = new Server[IO]
      ms: MessageStore[IO] = new MessageStoreLocalImpl[IO](ref)
      wsComponent = WSRoutesComponent.mkAsync[IO](ms)
      _ <- server.start(wsComponent)

    } yield ExitCode.Success
  }
}
