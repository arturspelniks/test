import java.util.UUID

import Master.Terminate
import akka.NotUsed
import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.{ActorAttributes, ActorMaterializer, OverflowStrategy, Supervision}
import akka.stream.scaladsl.{Flow, Sink, Source}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WsService(implicit system: ActorSystem) {
  val master = system.actorOf(Props[Master])

  def msgFlow: Flow[Message, Message, NotUsed] = {
    val sid: String = UUID.randomUUID().toString
    val parallelism = Runtime.getRuntime.availableProcessors

    implicit val materializer = ActorMaterializer()

    // transform websocket message to web message
    val strToWebMsg = Flow[String]
      .map{str =>
        if (str.length > 0)
          Master.NewMessage(Codec.decodeWebMsg(str).get, sid)
        else Master.NewMessage(BadMessage, sid)
      }

    val incomingMessages: Sink[Message, NotUsed] =
      Flow[Message].mapAsync(parallelism) {
        case tm: TextMessage =>
          val text = tm.toStrict(3 seconds).map(_.text)
          text
        case bm: BinaryMessage =>
          Future("")
      }
        .via(strToWebMsg)
        .withAttributes(ActorAttributes.supervisionStrategy(Supervision.resumingDecider)) //skips errors and processes further
        .to(Sink.actorRef[Master.NewMessage](master, Terminate(sid)))

    val outgoingMessages: Source[Message, NotUsed] =
      Source.actorRef[Client.ReceiveMessage](100, OverflowStrategy.fail) // actorRef does not work with backpressure
        .mapMaterializedValue {
          wsActor =>
            // give the user actor a way to send messages out
            println(s"[mapMaterializedValue.wsActor] ${wsActor.path}")
            master ! Master.AssignWsActorToSid(wsActor, sid)
            NotUsed
        }.map {
        // transform domain message to web message
        (receivedMessage: Client.ReceiveMessage) =>
          println(s"[Receive message] ${Codec.encodeWebMsg(receivedMessage.webMessage)}")
          TextMessage(Codec.encodeWebMsg(receivedMessage.webMessage))
      }

    // then combine both to a flow
    Flow.fromSinkAndSource(incomingMessages, outgoingMessages)
  }
}
