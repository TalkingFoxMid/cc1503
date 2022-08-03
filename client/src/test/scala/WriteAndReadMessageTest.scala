import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import ru.wdevs.cc1503.Requests.{CreateMessageDTO, InitSession, ReadMessages}
import ru.wdevs.cc1503.WebsocketClient.SendMessage
import ru.wdevs.cc1503.{WebsocketClient, WebsocketClientImpl}
import org.scalatest.matchers.should.Matchers

class WriteAndReadMessageTest extends AnyFlatSpec {
  "client" should "Write and read one message" in new IntegrationTest {

    override def action: IO[Unit] = for {
      cl <- WebsocketClient.make[IO](
        List(
          InitSession("amogus"),
          CreateMessageDTO("chat", "aaa"),
          ReadMessages("chat", 10)
        ).map(SendMessage)
      )
      data <- cl.run
      _ = data shouldBe
    } yield ()
  }
}
