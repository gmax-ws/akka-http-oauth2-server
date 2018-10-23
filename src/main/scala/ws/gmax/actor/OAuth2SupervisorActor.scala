package ws.gmax.actor

import akka.actor.{ActorRef, DeadLetter, Props, Terminated}
import ws.gmax.model._

class OAuth2SupervisorActor() extends AbstractSupervisorActor {

  val oauth2Actor: ActorRef = context.actorOf(Props[OAuth2Actor], "oauth2Actor")

  override def preStart(): Unit = {
    super.preStart()
    log.info("OAuth2 supervisor is up")
    context.watch(oauth2Actor)
  }

  override def postStop(): Unit = {
    log.info("OAuth2 supervisor is down")
    super.postStop()
  }

  override def receive: Receive = {
    case message: OAuth2Message => oauth2Actor forward message

    case message: VerifyTokenMessage => oauth2Actor forward message

    case message: GenerateKeyPair.type => oauth2Actor forward message

    case message: PublicKeyMessage.type => oauth2Actor forward message

    case DeadLetter(message, sender, recipient) =>
      log.warning(s"The $recipient is not able to process message $message received from $sender")

    case Terminated(actor) =>
      log.error(s"Stopping actor and shutting down system because of actor: ${actor.path}")
      context.stop(self)
      context.system.terminate

    case _ =>
      log.error("Message is not processed")
  }
}

object OAuth2SupervisorActor {
  def apply(): Props = Props(new OAuth2SupervisorActor())
}
