package com.wenbo.controllers

import actors.{BroadcastActor, ChatClientActor, ChatRoomActor}
import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem}
import akka.stream.scaladsl.{BroadcastHub, Flow, Keep, MergeHub, Sink, Source}
import akka.stream.{ActorMaterializer, KillSwitches, Materializer, UniqueKillSwitch}
import akka.util.ByteString
import play.api.http.websocket.{BinaryMessage, CloseCodes, CloseMessage, Message}
import javax.inject.{Inject, Named}
import play.api.Logger
import play.api.libs.json.Json
import play.api.libs.streams.{ActorFlow, AkkaStreams}
import play.api.mvc.WebSocket.MessageFlowTransformer
import play.api.mvc.{AbstractController, ControllerComponents, RequestHeader, WebSocket}
import views.ConvertHtml
import boopickle.Default._
import com.typesafe.config.ConfigException.Null
import com.wenbo.client.shared.SharedMessages.SharedMessages

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ChatController @Inject()(cc: ControllerComponents)(implicit actorSystem: ActorSystem, mat: Materializer, ec: ExecutionContext) extends AbstractController(cc) {

  val logger = play.api.Logger(getClass)

  protected val romMap = new scala.collection.mutable.HashMap[String, Flow[SharedMessages, SharedMessages, _]]

  implicit val webSocketTransformer = new MessageFlowTransformer[SharedMessages, SharedMessages] {
    override def transform(flow: Flow[SharedMessages, SharedMessages, _]): Flow[Message, Message, _] = {
      AkkaStreams.bypassWith[ Message, SharedMessages, Message ]( Flow[ Message ] collect {
        case BinaryMessage( data ) =>
          Unpickle.apply[SharedMessages].tryFromBytes( data.asByteBuffer ) match {
            case Success( msg ) =>
              Left( msg )
            case Failure( err ) =>
              Right( CloseMessage( CloseCodes.Unacceptable, s"Error with transfer: $err" ) )
          }
        case _ =>
          Right( CloseMessage( CloseCodes.Unacceptable, "This WebSocket only accepts binary." ) )
      })( flow.map { msg =>
        val bytes = ByteString.fromByteBuffer( Pickle.intoBytes[SharedMessages](msg) )
        BinaryMessage( bytes )
      })
    }
  }

  def chatPage = Action {
    Ok(ConvertHtml.getHtml).as(HTML)
  }

  def getFromMap(str: String) = {
    this.synchronized({
      if (!romMap.contains(str)) {
        val chatRoom = ActorFlow.actorRef[SharedMessages, SharedMessages](out => ChatRoomActor.props(out, Option[String](str)))
        val (hubSink: Sink[SharedMessages, NotUsed], hubSource: Source[SharedMessages, NotUsed]) = MergeHub.source[SharedMessages](16).via(chatRoom).toMat(BroadcastHub.sink(256))(Keep.both).run()
//        val (hubSink: Sink[SharedMessages, NotUsed], hubSource: Source[SharedMessages, NotUsed]) = MergeHub.source[SharedMessages](16).toMat(BroadcastHub.sink(256))(Keep.both).run()

        val roomChatFlow: Flow[SharedMessages, SharedMessages, _] = {
          Flow.fromSinkAndSource(hubSink, hubSource).joinMat((KillSwitches.singleBidi[SharedMessages, SharedMessages]))(Keep.right).backpressureTimeout(3 second)
        }

        romMap.put(str,  roomChatFlow)
      }
      romMap.getOrElse(str, null)
    })
  }


  def chat: WebSocket = WebSocket.acceptOrResult[SharedMessages, SharedMessages] {
    case rh if sameOriginCheck(rh) =>
      val user = rh.queryString("user").headOption
      val room = rh.queryString("room").headOption
      val roomChatFlow = getFromMap(room.get)
      val chatClient = ActorFlow.actorRef[SharedMessages, SharedMessages](out => ChatClientActor.props(out, user))
      val flow = chatClient.viaMat(roomChatFlow)(Keep.right)
      Future.successful(flow).map { flow =>
        Right(flow)
      }.recover {
        case e: Exception =>
          logger.error("cannot create websocket", e)
          val jsError = Json.obj("error" -> "cannot create websocket")
          val result = InternalServerError(jsError)
          Left(result)
      }

    case rejected =>
      logger.error(s"Request ${rejected}")
      Future.successful {
        Left(Forbidden("forbidden"))
      }
  }
  /**
    * Checks that the WebSocket comes from the same origin.  This is necessary to protect
    * against Cross-Site WebSocket Hijacking as WebSocket does not implement Same Origin Policy.
    *
    * See https://tools.ietf.org/html/rfc6455#section-1.3 and
    * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
    */
  def sameOriginCheck(rh: RequestHeader): Boolean = {
    rh.headers.get("Origin") match {
      case Some(originValue) if originMatches(originValue) =>
        logger.debug(s"originCheck: originValue = $originValue")
        true

      case Some(badOrigin) =>
        logger.error(s"originCheck: rejecting request because Origin header value ${badOrigin} is not in the same origin")
        false

      case None =>
        logger.error("originCheck: rejecting request because no Origin header found")
        false
    }
  }

  /**
    * Returns true if the value of the Origin header contains an acceptable value.
    *
    * This is probably better done through configuration same as the allowedhosts filter.
    */
  def originMatches(origin: String): Boolean = {
    //    origin.contains("localhost:9000") || origin.contains("localhost:19001")
    true
  }
}