package ws.gmax.actor

import akka.actor.{ActorRef, DeadLetter, Props, Terminated}
import ws.gmax.model._

class JwtSupervisorActor() extends AbstractSupervisorActor {

  val jwtActor: ActorRef = context.actorOf(Props[JwtActor], "jwtActor")

  override def preStart(): Unit = {
    super.preStart()
    log.info("JWT supervisor is up")
    context.watch(jwtActor)
  }

  override def postStop(): Unit = {
    log.info("JWT supervisor is down")
    super.postStop()
  }

  override def receive: Receive = {
    case message: OAuth2Message => jwtActor forward message
      
    case message: VerifyTokenMessage => jwtActor forward message

    case message: GenerateKeyPair.type => jwtActor forward message

    case message: PublicKeyMessage.type => jwtActor forward message

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

object JwtSupervisorActor {
  def apply(): Props = Props(new JwtSupervisorActor())
}
