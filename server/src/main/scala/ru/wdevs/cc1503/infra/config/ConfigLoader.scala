package ru.wdevs.cc1503.infra.config

import cats.effect.kernel.Sync
import cats.effect.{ExitCode, IO, IOApp}
import net.ceedubs.ficus.Ficus._
import com.typesafe.config.{Config, ConfigFactory}
import AppConfig.AppConfig
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import ru.wdevs.cc1503.infra.config.AppConfig.AppConfig

trait ConfigLoader[F[_]] {
  def loadConfig: F[AppConfig]
}

class ConfigLoaderImpl[F[_]: Sync] extends ConfigLoader[F] {
  override def loadConfig: F[AppConfig] = Sync[F].delay {
    val config: Config = ConfigFactory.load()
    config.as[AppConfig]
  }
}