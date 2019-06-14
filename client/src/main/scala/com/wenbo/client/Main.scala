package com.wenbo.client

import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{Element, HTMLTextAreaElement}
import com.wenbo.chat.ChatPage
import scala.scalajs.js.timers.setTimeout

object Main {

  def main(args: Array[String]): Unit = {
    var route = (document.location.href.substring(document.location.protocol.size + 2 + document.location.host.size, document.location.href.length - document.location.search.length))
    if (route == "/" || route == "" || route.startsWith("/?")) {
      //创建一个标签
      var input = dom.document.createElement("textarea").asInstanceOf[HTMLTextAreaElement]
      dom.document.body.appendChild(input)
      var button = dom.document.createElement("button")
      dom.document.body.appendChild(button)
    } else if (route == "/chatPage" || route.startsWith("/chatPage?")) {
      println(route)
      ChatPage.run
    }
  }
}
