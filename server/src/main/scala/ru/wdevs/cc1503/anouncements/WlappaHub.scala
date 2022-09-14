package ru.wdevs.cc1503.anouncements

import cats.effect.kernel.Concurrent
import cats.effect.std.Queue
import cats.{Monad, Parallel}
import cats.effect.{ExitCode, IO, IOApp, Ref}
import cats.syntax.all._
import fs2.{Pull, Stream}

class WlappaHub[F[_]: Concurrent: Parallel, A](private val listenersF:  Ref[F, List[A => F[Unit]]]) {

  def put(a: A): F[Unit] =
    for {
      listeners <- listenersF.get
      _ <- listeners.parTraverse(_(a))

    } yield ()

  private def subscribeQueue(queue: Queue[F, A]): F[Unit] =
    for {
      _ <- listenersF.update(
        list => (queue.offer _) :: list
      )
    } yield ()

  val subscribedStream: Stream[F, A] = {
    for {
      q <- Pull.eval(Queue.unbounded[F, A])
      _ <- Pull.eval(subscribeQueue(q))
      _ <- Pull
        .loop[F, A, Unit](_ =>
          for {
            el <- Pull.eval(q.take)
            _ <- Pull.output1(el)
          } yield Some(())
        )
        .apply(())
    } yield ()
  }.stream

}

object WlappaHub {
  def mk[F[_]: Concurrent: Parallel, A]: F[WlappaHub[F, A]] =
    for {
      listeners <- Ref.of[F, List[A => F[Unit]]](List.empty[A => F[Unit]])
    } yield new WlappaHub[F, A](listeners)
}

object WlappaHubTest extends IOApp {
  import scala.concurrent.duration._
  override def run(args: List[String]): IO[ExitCode] =
    for {
      listeners <- Ref.of[IO, List[Int => IO[Unit]]](List.empty[Int => IO[Unit]])
      wlappaHub = new WlappaHub(listeners)
      eventsStream = wlappaHub.subscribedStream
      _ <- eventsStream.map(println _)
        .compile.drain.start
      _ <- eventsStream.map(println _)
        .compile.drain.start

      _ <- List.range(0, 50).traverse(
        d => for {
          _ <- IO.sleep(3.seconds)
          _ <- wlappaHub.put(d)
        } yield ()
      )
    } yield ExitCode.Success
}