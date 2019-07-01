package com.wenbo.client.shared

object SharedMessages {
  sealed trait SharedMessages
  case class Join(s: String) extends SharedMessages
  case class Broadcast(s: String, c: String) extends SharedMessages
  case class Leave(s:String) extends SharedMessages
}


