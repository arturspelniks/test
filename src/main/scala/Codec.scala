import io.circe.{HCursor}
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._

object Codec {

  def decodeWebMsg(jsonString: String): Option[WebMessage] = {
    parse(jsonString) match {
      case Left(failure) =>
        println("Invalid JSON :( " + failure.getMessage)
        None
      case Right(json) =>
        val cursor: HCursor = json.hcursor
        val msgType = cursor.get[String]("msgType")
        if (msgType.toOption.get == "create")
          json.as[CreateClientMessage].toOption
        else
          json.as[NormalMessage].toOption
    }
  }

  def encodeWebMsg(msg: WebMessage): String = {
    msg match {
      case nMsg: NormalMessage =>
        nMsg.asJson.toString()
      case cMsg: CreateClientMessage =>
        cMsg.asJson.toString()
      case _ =>
        msg.asJson.toString()
    }
  }
}
