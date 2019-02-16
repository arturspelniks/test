import akka.NotUsed
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.{Directives, Route}
import akka.stream.scaladsl.Flow

trait Router {
  def route: Route
}

class WsApiRouter(credentialsValidator: CredentialsValidator, msgFlow: () => Flow[Message, Message, NotUsed]) extends Router with Directives{
  def route: Route =
    path("ws_api") {
      println("[ws_api] url has been reached")
      authenticateOrRejectWithChallenge(credentialsValidator.myUserPassAuthenticator _) {
        userName =>
          println(s"[User Connected] $userName")
          handleWebSocketMessages(msgFlow.apply())
      }
    }
}
