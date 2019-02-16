import io.circe.{HCursor}
import io.circe.parser._
import io.circe.generic.auto._
import io.circe.syntax._
import org.mindrot.jbcrypt.BCrypt

case class L2 (in: String, un: String)

sealed trait WebMessages
case class M1(msgType: String = "normal", ad: List[String]) extends WebMessages
case class M2 (name: String, msgType: String = "create") extends WebMessages
case class M3(msgType: String = "normal", inside: L2) extends WebMessages


val msg = M1("normal", List("elem1", "elem2"))
val msg3 = M3("normal", L2("in","un"))

val msgString: String = """
  {
    "msgType": "test",
    "inside": {"in" : "in", "un" : "un"}
  }
"""

def encodeWebMsg(msg: WebMessages): String = {
  msg.asJson.toString()
}

def decodeWebMsg(jsonString: String): Option[WebMessages] = {
  parse(jsonString) match {
    case Left(failure) =>
      println("Invalid JSON :( " + failure.getMessage)
      None
    case Right(json) =>
      json.as[M3].toOption
  }
}

println(encodeWebMsg(msg3))
println(decodeWebMsg(msgString))

/*
val hashMe = "admin"
//val bcrypt = hashMe.bcrypt
var hashed = "$2a$10$jUWLpy4Uv2bdFDpUe7WdqeKPQg6Qa9zRFv95rLjvG0KYHa3MliU.6"
val candidate = BCrypt.hashpw(hashMe, BCrypt.gensalt())

import org.mindrot.jbcrypt.BCrypt

if (BCrypt.checkpw(hashMe, hashed)) System.out.println("It matches")
else System.out.println("It does not match")
//println("BCrypt Matches: " + (hashMe.bcrypt hash= bcrypt) )
*/
