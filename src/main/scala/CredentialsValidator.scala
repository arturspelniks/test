import akka.actor.ActorSystem
import akka.http.scaladsl.model.HttpCharsets.`UTF-8`
import akka.http.scaladsl.model.headers.{HttpChallenge, HttpCredentials}
import akka.http.scaladsl.server.Directives._
import akka.parboiled2.util.Base64
import scala.concurrent.Future
import org.mindrot.jbcrypt.BCrypt

class CredentialsValidator(userCredentialsMap: Map[String, String])(implicit system: ActorSystem) {
  import system.dispatcher

  val challenge = HttpChallenge("Basic", "WebSocket API realm")

  def encrypt(s: String): String = s.reverse

  def extractCredentials(creds: HttpCredentials): (String, String) = {
    val bytes = Base64.rfc2045.decodeFast(creds.token())
    val userPass = new String(bytes, `UTF-8`.nioCharset)
    val credentials: (String, String) = userPass.indexOf(':') match {
      case -1 => (userPass, "")
      case ix => (userPass.substring(0, ix), userPass.substring(ix + 1))
    }

    credentials
  }

  def auth(creds: HttpCredentials): Boolean = {
    verifyCredentials(extractCredentials(creds))
  }

  def myUserPassAuthenticator(credentials: Option[HttpCredentials]): Future[AuthenticationResult[String]] =
    Future {
      credentials match {
        case Some(creds) =>
          if (auth(creds)) Right(getUsername(creds))
          else Left(challenge)
        case _ =>
          Left(challenge)
      }
    }

  def getUsername(creds: HttpCredentials): String = {
    extractCredentials(creds)._1
  }

  def verifyCredentials(credentials: (String, String)): Boolean = {
    userCredentialsMap.get(encrypt(credentials._1)) match {
      case Some(pwd) =>
        BCrypt.checkpw(credentials._2, pwd)
      case None =>
        false
    }
  }
}
