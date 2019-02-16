import akka.actor.{Actor, ActorLogging, ActorRef}

object Client {
  case class Connected(sid: String)
  case class SendMessage(webMessage: WebMessage)
  case class ReceiveMessage(webMessage: WebMessage)
  case class ConnectToWsActor(wsActor: ActorRef, sid: String)
}

class Client(master: ActorRef, name: String) extends Actor with ActorLogging{
  import Client._
  import Master._

  override def receive: Receive = {
    case Connected(sid) =>
      log.info(s"[Connected]")
      context.become(connectedToSession(sid))
  }

  def connectedToSession (sid: String): Receive = {
    log.info(s"[Client connected] $name to $sid")
    master ! ClientConnected (sid)

    {
      case ConnectToWsActor(wsActor, sidFromMaster) =>
        log.info(s"[ConnectToWsActor] $wsActor")
        if (sid == sidFromMaster) {
          log.info(s"[becoming connected to actor] sid = $sid wsActor = $wsActor")
          context.become(connectedToWsActor(wsActor, sid))
          wsActor ! ReceiveMessage(ClientCreatedMessage(name, "successfully created"))
        }
        else
          log.info(s"[...] $sid != $sidFromMaster")
    }
  }

  def connectedToWsActor (wsActor: ActorRef, sid: String): Receive = {
    case ReceiveMessage(webMessage) =>
      log.info(s"[Client $name] received a message $webMessage and will send it to $wsActor")
      wsActor ! ReceiveMessage(webMessage)
  }

}