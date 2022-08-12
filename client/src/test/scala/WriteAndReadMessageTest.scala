import cats.effect.IO
import cats.syntax.all._
import org.scalatest.flatspec.AnyFlatSpec
import ru.wdevs.cc1503.Requests.{CreateMessageDTO, InitSession, ReadMessages}
import ru.wdevs.cc1503.WebsocketClient.SendMessage
import ru.wdevs.cc1503.{WebsocketClient, WebsocketClientImpl}
import org.scalatest.matchers.should.Matchers._
import ru.wdevs.cc1503.Responses._

class WriteAndReadMessageTest extends AnyFlatSpec {
  "client" should "Write and read one message" in new IntegrationTest {

    override def action: IO[Unit] = for {
      chatId <- randomChatId
      cl <- WebsocketClient.make[IO](
        List(
          InitSession("amogus"),
          CreateMessageDTO(chatId, "aaa"),
          ReadMessages(chatId, 10)
        ).map(SendMessage)
      )
      data <- cl.run
    } yield {
      data shouldBe List(SessionWasInitialized(), MessageSaved(), MessageHistory(chatId, List(ChatMessage("aaa","amogus"))))
    }
  }

  "two clients" should "Write and read one message" in new IntegrationTest {

    override def action: IO[Unit] = for {
      chatId <- randomChatId
      cl1 <- WebsocketClient.make[IO](
        List(
          InitSession("amogus1"),
          CreateMessageDTO(chatId, "aaa")
        ).map(SendMessage)
      )

      cl2 <- WebsocketClient.make[IO](
        List(
          InitSession("amogus2"),
          ReadMessages(chatId, 10)
        ).map(SendMessage)
      )
      data1 <- cl1.run
      data2 <- cl2.run
    } yield {
      data1 shouldBe List(SessionWasInitialized(), MessageSaved())
      data2 shouldBe List(SessionWasInitialized(), MessageHistory(chatId, List(ChatMessage("aaa","amogus1"))))
    }
  }


}
