import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Terminated}

object Master {
  case class Assign(name: String, client: ActorRef, sid: String)
  case class NewMessage (webMessage: WebMessage, sid: String)
  case class AssignWsActorToSid (wsActor: ActorRef, sid: String)
  case class ClientConnected(sid: String)
  case class Terminate(sid: String)
  case object Initialize
}

class Master extends Actor with ActorLogging {
  import Master._
  import Client._

  override def preStart(): Unit = {
    self ! Initialize
  }

  override def receive: Receive = {
    case Initialize =>
      context.become(initialized())
  }

  def initialized(clients: Map[String, (ActorRef, String)] = Map.empty, wsActorsPerSid: Map[String, ActorRef] = Map.empty): Receive = {
    case Assign(name, client, sid) =>
      log.info(s"[Assign client] $name")
      val newClients = clients + (name -> (client, sid))
      context.watch(client)
      client ! Connected(sid)
      context.become(initialized(newClients, wsActorsPerSid))
    case Terminated(client) =>
      log.info(s"[Removing client] $client")
      val foundClient = clients.find(_._2._1 == client)
      foundClient match {
        case Some(cl) =>
          val newClients = clients - cl._1
          context.become(initialized(newClients, wsActorsPerSid))
        case None => log.info(s"[Nothing to remove - client does not exist] $client")
      }
    case Terminate(sid) =>
      log.info(s"[Terminating session] $sid")
      val newWsActorsPerSid = wsActorsPerSid - sid
      context.become(initialized(clients, newWsActorsPerSid))
      val foundClient = clients.find(_._2._2 == sid)
      foundClient match {
        case Some(cl) =>
          cl._2._1 ! PoisonPill
        case None => log.info(s"[Nothing to terminate - session does not exist] $sid")
      }
    case NewMessage(webMessage, sid) =>
      log.info(s"[New message] $webMessage")
      log.info(s"[clients] $clients")

      webMessage match {
        case nMsg: NormalMessage =>
          log.info(s"[RECEIVED NORMAL MESSAGE] $nMsg")
          val toClientName = nMsg.to
          log.info(s"[toClientName] $toClientName")
          clients.get(toClientName) match {
            case Some(found) =>
              log.info(s"[toClientActor] ${found._1}")
              found._1 ! ReceiveMessage(ReplyMessage(nMsg.message, nMsg.from))
            case None =>
              wsActorsPerSid.get(sid).get ! ReceiveMessage(NoSuchClientMessage(toClientName, "no such client"))
          }
        case cMsg: CreateClientMessage =>
          val userActor = context.actorOf(Props(new Client(self, cMsg.name)), cMsg.name)
          self ! Assign(cMsg.name, userActor, sid)
        case BadMessage =>
          log.info("[Binary, empty or another unsupported message came into] ignore")
      }
    case AssignWsActorToSid(wsActor, sid) =>
      log.info(s"[new sid -> wsActor]")
      val newWsActorsPerSid = wsActorsPerSid + (sid -> wsActor)
      context.become(initialized(clients, newWsActorsPerSid))
    case ClientConnected(sid) =>
      // assign ws actor to client
      log.info(s"[assign actor to client]")
      sender() ! ConnectToWsActor(wsActorsPerSid.get(sid).get, sid)
  }
}
