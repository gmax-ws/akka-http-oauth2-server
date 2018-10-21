package ws.gmax.actor

import akka.actor.SupervisorStrategy.Resume
import akka.actor.{Actor, ActorLogging, DeadLetter, OneForOneStrategy}

abstract class AbstractSupervisorActor extends Actor with ActorLogging {

  override def supervisorStrategy: OneForOneStrategy = OneForOneStrategy() {

    case ex: Exception =>
      log.warning(s"general exception: ${ex.getMessage}")
      ex.printStackTrace()
      Resume
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    message foreach { msg =>
      log.warning(s"preRestart: actor $msg Reason ${reason.getMessage}")
    }
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable): Unit = {
    log.warning(s"postRestart: actor restarted due to ${reason.getMessage}")
    super.postRestart(reason)
  }

  override def preStart(): Unit = {
    log.warning("preStart: actor is started")
    context.system.eventStream.subscribe(self, classOf[DeadLetter])
  }

  override def postStop(): Unit = {
    log.warning("postStop: actor is stopped")
    context.system.eventStream.unsubscribe(self)
  }
}