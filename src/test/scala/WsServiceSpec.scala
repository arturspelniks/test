import akka.http.scaladsl.server.{Directives}
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.model.ws.{BinaryMessage}
import akka.util.ByteString
import Codec._

import scala.concurrent.duration._

class WsServiceSpec extends WordSpec with Matchers with ScalatestRouteTest with Directives {
  val credentialsValidator = new CredentialsValidator(CredentialsRepository.getCredentialsMap)
  val wsService = new WsService
  val router = new WsApiRouter(credentialsValidator, () => wsService.msgFlow)
  val validCredentials = BasicHttpCredentials("admin", "admin")
  val wsClient = WSProbe()

  "WsService" should {
   "create new actor on create request, send and receive message" in {
     WS("/ws_api", wsClient.flow) ~> addCredentials(validCredentials) ~> router.route ~>
       check {
         // check response for WS Upgrade headers
         isWebSocketUpgrade shouldEqual true

         for (i <- 1 to 10) {
           wsClient.sendMessage(encodeWebMsg(CreateClientMessage(s"client_$i", "create")))
           wsClient.expectMessage(encodeWebMsg(ClientCreatedMessage(s"client_$i", "successfully created")))
           wsClient.sendMessage(encodeWebMsg(NormalMessage(s"$i", "TEST", s"client_$i", s"client_$i", "normal")))
           wsClient.expectMessage(encodeWebMsg(ReplyMessage("TEST", s"client_$i")))
         }

         wsClient.sendMessage(BinaryMessage(ByteString("abcdef")))
         wsClient.expectNoMessage(100 millis)

         val normalMessage1 = NormalMessage("1", "TEST", "client1", "client2", "normal")
         val replyMessage1 = NoSuchClientMessage("client2", "no such client")

         wsClient.sendMessage(encodeWebMsg(normalMessage1))
         wsClient.expectMessage(encodeWebMsg(replyMessage1))
       }
   }
  }
}
