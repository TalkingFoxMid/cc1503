package ru.wdevs.cc1503.handling

import cats.effect.Resource
import io.grpc.ServerServiceDefinition

trait GrpcHandler[F[_]] {
  val srvcDef: Resource[F, ServerServiceDefinition]
}
