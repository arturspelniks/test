sealed trait WebMessage

// in
case class NormalMessage(id: String, message: String, from: String, to: String, msgType: String = "normal") extends WebMessage
case class CreateClientMessage (name: String, msgType: String = "create") extends WebMessage
// out
case class ClientCreatedMessage (name: String, message: String) extends  WebMessage
case class ReplyMessage(message: String, from: String) extends WebMessage
case class NoSuchClientMessage(clientName: String, message: String) extends WebMessage
case object BadMessage extends WebMessage