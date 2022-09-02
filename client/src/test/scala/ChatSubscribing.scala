import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import ru.wdevs.cc1503.Requests.{CreateMessageDTO, InitSession, ReadMessages, SubscribeChat}
import ru.wdevs.cc1503.Responses.{ChatMessage, MessageHistory, MessageSaved, SessionWasInitialized, SubscribedToChat}
import ru.wdevs.cc1503.WebsocketClient
import ru.wdevs.cc1503.WebsocketClient.SendMessage
import scala.concurrent.duration._

class ChatSubscribing extends AnyFlatSpec {
  "client" should "Subscribe to chat" in new IntegrationTest {

    override def action: IO[Unit] = for {
      chatId <- randomChatId
      cl <- WebsocketClient.make[IO](
        List(
          InitSession("amogus"),
          SubscribeChat(chatId)
        ).map(SendMessage),
        readDuration = 1.hour
      )
      data <- cl.run
    } yield {
      data shouldBe List(SessionWasInitialized(), SubscribedToChat(chatId))
    }
  }
}
