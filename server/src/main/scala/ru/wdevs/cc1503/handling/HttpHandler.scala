package ru.wdevs.cc1503.handling

import org.http4s.HttpRoutes

trait HttpHandler[F[_]] {
  def routes: (String, HttpRoutes[F])
}
