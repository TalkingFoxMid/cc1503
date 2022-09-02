package ru.wdevs.cc1503.infra.config

object AppConfig {
  case class AppConfig(
    nodes: Map[String, String],
    id: String
  )
}
