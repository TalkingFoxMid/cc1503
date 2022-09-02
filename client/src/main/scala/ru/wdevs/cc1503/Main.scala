package ru.wdevs.cc1503


import cats.Id
import fs2.{Pipe, Pull, Stream}

object WebSocketStreamFs2 extends App {

  val pWithContext = Pull.pure(190)
    .flatMap (
      r => Pull.output1(r).map(_ => 99).void
    )
  val s1 = pWithContext.stream
  val list = s1.compile.toList
  println(list)
}