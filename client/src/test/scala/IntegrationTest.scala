import cats.effect.IO
import cats.effect.unsafe.IORuntime
import org.scalatest.flatspec.AnyFlatSpec
import sttp.capabilities.fs2.Fs2Streams

trait IntegrationTest {
  private val runtime = IORuntime.global
  implicit val fs2 = Fs2Streams[IO]

  def action: IO[Unit]

  action.unsafeRunSync()(runtime)
}
