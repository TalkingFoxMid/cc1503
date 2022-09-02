package ru.wdevs.cc1503.config

import cats.effect.kernel.Sync
import cats.effect.{ExitCode, IO, IOApp}
import net.ceedubs.ficus.Ficus._
import com.typesafe.config.{Config, ConfigFactory}
import ru.wdevs.cc1503.config.AppConfig.AppConfig
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

trait ConfigLoader[F[_]] {
  def loadConfig: F[AppConfig]
}

class ConfigLoaderImpl[F[_]: Sync] extends ConfigLoader[F] {
  override def loadConfig: F[AppConfig] = Sync[F].delay {
    val config: Config = ConfigFactory.load()
    config.as[AppConfig]
  }
}

object T extends IOApp {
  val cl = new ConfigLoaderImpl[IO]

  override def run(args: List[String]): IO[ExitCode] =
    for {
      cfg <- cl.loadConfig
      _ = println(cfg)
    } yield ExitCode.Success
}