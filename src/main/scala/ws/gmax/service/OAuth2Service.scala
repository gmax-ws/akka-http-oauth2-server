package ws.gmax.service

import akka.actor.ActorRef
import akka.pattern.ask
import com.typesafe.config.Config
import ws.gmax._
import ws.gmax.model._
import ws.gmax.routes.OAuth2Routes

import scala.concurrent.Future

class OAuth2Service(jwtSupervisorActor: ActorRef, val config: Config) extends OAuth2Routes {

  def oauth2Request(request: OAuth2Message): Future[Any] =
    jwtSupervisorActor ? request

  def validateToken(token: String): Future[Any] =
    jwtSupervisorActor ? VerifyTokenMessage(token)

  def getPublicKey: Future[Any] =
    jwtSupervisorActor ? PublicKeyMessage

  def generateKeys: Future[Any] =
    jwtSupervisorActor ? GenerateKeyPair
}

object OAuth2Service {
  def apply(jwtSupervisorActor: ActorRef)(implicit config: Config): OAuth2Service =
    new OAuth2Service(jwtSupervisorActor, config)
}
