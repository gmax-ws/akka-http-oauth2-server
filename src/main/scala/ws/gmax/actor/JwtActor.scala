package ws.gmax.actor

import akka.actor.{Actor, ActorLogging}
import ws.gmax.jwt.JwtToken
import ws.gmax.model._

class JwtActor extends Actor with ActorLogging {

  val jwt = JwtToken(context.system.settings.config)

  override def receive: Receive = {
    case request: OAuth2Message => sender ! jwt.issue(request)
    case VerifyTokenMessage(token) => sender ! jwt.validate(token)
    case GenerateKeyPair => sender ! jwt.pemKeys()
    case PublicKeyMessage => sender ! jwt.publicKey
  }
}
