import cats.effect.IO
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers._
import ru.wdevs.cc1503.Requests.{CreateMessageDTO, InitSession, ReadMessages, SubscribeChat}
import ru.wdevs.cc1503.Responses._
import ru.wdevs.cc1503.WebsocketClient
import ru.wdevs.cc1503.WebsocketClient.SendMessage

import scala.concurrent.duration.DurationInt

class EventsExchangingTest extends AnyFlatSpec {
  "client" should "Write and read one message" in new IntegrationTest {

    override def action: IO[Unit] = for {
      chatId <- randomChatId
      cl1 <- WebsocketClient.make[IO](
        List(
          InitSession("amogus1")
        ).map(SendMessage)
      )
      cl2 <- WebsocketClient.make[IO](
        List(InitSession("amogus1")).map(SendMessage)
      )
      _ <- cl1.run
      _ <- cl2.run
      _ <- cl1.triggerAction(SubscribeChat(chatId))
      _ <- cl2.triggerAction(CreateMessageDTO(chatId, "text11"))
      _ <- IO.sleep(1.seconds)
      data1 <- cl1.fetchData
      data2 <- cl2.fetchData
    } yield {
      data2 shouldBe List(SessionWasInitialized())
    }
  }


}
