import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{AuthenticationFailedRejection, Directives}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.AuthenticationFailedRejection.{CredentialsMissing, CredentialsRejected}

class AuthenticatorSpec extends WordSpec with Matchers with ScalatestRouteTest with Directives {
  val credentialsValidator = new CredentialsValidator(CredentialsRepository.getCredentialsMap)
  val wsService = new WsService
  val router = new WsApiRouter(credentialsValidator, () => wsService.msgFlow)

  "Authenticator" should {
    "reject invalid connection" in {
      WS("/ws_api", wsService.msgFlow) ~> router.route ~> check {
        rejection shouldEqual AuthenticationFailedRejection(CredentialsMissing, credentialsValidator.challenge)
      }
    }

    "reject wrong credentials" in {
      val invalidCredentials = BasicHttpCredentials("qwe", "rty")
      WS("/ws_api", wsService.msgFlow) ~> addCredentials(invalidCredentials) ~> router.route ~> check {
        rejection shouldEqual AuthenticationFailedRejection(CredentialsRejected, credentialsValidator.challenge)
      }
    }

    "accept valid credentials" in {
      val validCredentials = BasicHttpCredentials("admin", "admin")
      WS("/ws_api", wsService.msgFlow) ~> addCredentials(validCredentials) ~> router.route ~> check {
        status shouldEqual StatusCodes.SwitchingProtocols
        isWebSocketUpgrade shouldEqual true
      }
    }
  }
}
