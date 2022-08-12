import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.scalatest.flatspec.AnyFlatSpec
import sttp.capabilities.fs2.Fs2Streams

import scala.util.Random

trait IntegrationTest {
  private val runtime = IORuntime.global
  implicit val fs2 = Fs2Streams[IO]
  private val rng = new Random()

  def action: IO[Unit]

  def randomChatId: IO[String] =
    IO {
      rng.alphanumeric.take(50).mkString
    }

  action.unsafeRunSync()(runtime)
}
