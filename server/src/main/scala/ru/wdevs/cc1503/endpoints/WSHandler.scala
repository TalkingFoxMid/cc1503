package ru.wdevs.cc1503.endpoints

import cats.effect.kernel.Sync
import fs2.Pipe
import sttp.capabilities.WebSockets
import sttp.capabilities.fs2.Fs2Streams
import sttp.tapir.PublicEndpoint

abstract class WSHandler[F[_], In, Out] {
  protected val sync: Sync[F]

  protected val wsEndpoint: PublicEndpoint[Unit, Unit, Pipe[F, In, Out], Fs2Streams[F] with WebSockets]

  protected val pipe: Pipe[F, In, Out]

  def buildWithLogic = wsEndpoint.serverLogicSuccess[F](
    _ => sync.delay(pipe)
  )
}
