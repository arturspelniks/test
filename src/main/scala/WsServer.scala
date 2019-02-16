import akka.actor.{ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer}

import scala.util.{Failure, Success}

object WsServer extends App {
  val host = "localhost"
  val port = 9000

  implicit val system: ActorSystem = ActorSystem(name = "jsonWs")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  import system.dispatcher

  val credentialsValidator = new CredentialsValidator(CredentialsRepository.getCredentialsMap)
  val wsService = new WsService
  val router = new WsApiRouter(credentialsValidator, () => wsService.msgFlow)

  val binding = Http().bindAndHandle(router.route, host, port)
  binding.onComplete{
    case
      Success(value) => println(s"[Started Server] $value")
    case
      Failure(exception) => println(exception.getMessage)
      system.terminate()
  }
}
