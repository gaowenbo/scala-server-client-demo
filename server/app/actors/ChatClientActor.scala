package actors

import com.wenbo.client.shared.SharedMessages._
import akka.actor._
import play.api.libs.json.JsValue

class ChatClientActor(out: ActorRef, user: Option[String]) extends Actor with ActorLogging {
  def receive: Receive = {
    case Broadcast(s, c) => {
      if (s == user.get) {
        out ! Broadcast(s, c)
      }
    }
  }

  override def preStart(): Unit = {
    user match {
      case Some(u) => {
        out ! Join(u)
      }
      case None =>
        log.info(s"No user name ${out.path}")
        self ! PoisonPill
    }
  }

  override def postStop(): Unit = {
    user match {
      case Some(u) => out ! Leave(u)
      case None => log.info(s"Stop ${out.path}")
    }
    self ! PoisonPill
  }
}

object ChatClientActor{
  def props(out: ActorRef, user: Option[String]) = Props(classOf[ChatClientActor], out, user)
}
