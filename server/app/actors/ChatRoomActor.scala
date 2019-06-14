package actors

import akka.actor._
import akka.http.scaladsl.model.ws.Message
import com.wenbo.client.shared.SharedMessages._

class ChatRoomActor(out: ActorRef, room: Option[String]) extends Actor with ActorLogging {
  def receive: Receive = {
    case msg => {
        out ! msg
    }
  }
}
object ChatRoomActor{
  def props(out: ActorRef, room: Option[String]) = Props(classOf[ChatRoomActor], out, room)
}

