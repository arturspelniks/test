import akka.NotUsed
import akka.http.scaladsl.server.Directives
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString

import scala.concurrent.duration._

class WsApiRouterSpec extends WordSpec with Matchers with ScalatestRouteTest with Directives {

  def greeter: Flow[Message, Message, NotUsed] =
    Flow[Message].mapConcat {
      case tm: TextMessage =>
        TextMessage(Source.single("Hello ") ++ tm.textStream ++ Source.single("!")) :: Nil
      case bm: BinaryMessage =>
        // ignore binary messages but drain content to avoid the stream being clogged
        bm.dataStream.runWith(Sink.ignore)
        Nil
    }

  val credentialsValidator = new CredentialsValidator(CredentialsRepository.getCredentialsMap)
  val wsService = new WsService
  val validCredentials = BasicHttpCredentials("admin", "admin")
  val websocketRoute = new WsApiRouter(credentialsValidator, () => greeter)

  val wsClient = WSProbe()

  "WsApiRouter" should {
    "correctly handle connection and messages" in {
      WS("/ws_api", wsClient.flow) ~> addCredentials(validCredentials) ~> websocketRoute.route ~>
        check {
          // check response for WS Upgrade headers
          isWebSocketUpgrade shouldEqual true

          // manually run a WS conversation
          wsClient.sendMessage("Peter")
          wsClient.expectMessage("Hello Peter!")

          wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
          wsClient.expectNoMessage(100.millis)

          wsClient.sendMessage("John")
          wsClient.expectMessage("Hello John!")

          wsClient.sendCompletion()
          wsClient.expectCompletion()
        }
    }
  }
}
