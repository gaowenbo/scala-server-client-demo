package com.wenbo.chat

import com.wenbo.client.shared.SharedMessages._
import org.scalajs.dom
import org.scalajs.dom._
import org.scalajs.dom.raw.{HTMLButtonElement, HTMLInputElement, HTMLParagraphElement}
import scala.scalajs.js.typedarray.{ArrayBuffer, TypedArrayBuffer}
import scala.scalajs.js.typedarray.TypedArrayBufferOps._


object ChatPage {
    var joinButton = dom.document.getElementById("join").asInstanceOf[HTMLButtonElement]
    var sendButton = dom.document.getElementById("send").asInstanceOf[HTMLButtonElement]

    def run = {
        var index = document.location.href.indexOf("?")
        var nameField = dom.document.getElementById("name").asInstanceOf[HTMLInputElement]

        joinButton.onclick = {event =>
          if (index > 0) {
            joinChat(nameField.value, document.location.href.substring(index + 1))
          } else {
            joinChat(nameField.value, "")
          }
          event.preventDefault()
        }
        nameField.focus()
        nameField.onkeypress = {event =>
          if (event.keyCode == 13) {
              joinButton.click()
              event.preventDefault()
          }
        }
    }

  def getWebsocketUrl(document: html.Document, name: String, room: String): String = {
    var wsProtocol = if (dom.document.location.protocol == "https:") "wss:" else "ws:"
    s"$wsProtocol//${dom.document.location.host}/chat?user=$name&room=$room"
  }

  def joinChat(name: String, room: String) = {
      joinButton.disabled = true;
      var playground = dom.document.getElementById("playground")
      playground.appendChild(p(s"${name}登录中。。。"))

      var chat = new WebSocket(getWebsocketUrl(dom.document, name, room))

      chat.binaryType = "arraybuffer"
      chat.onopen = {e =>
        playground.insertBefore(p("连接成功！"), playground.firstChild)
        sendButton.disabled = false
        var messageField = dom.document.getElementById("message").asInstanceOf[HTMLInputElement]
        messageField.focus()
        messageField.onkeypress = {event =>
          if (event.keyCode == 13) {
            sendButton.click()
            event.preventDefault()
          }
        }

        sendButton.onclick = {event =>
          import boopickle.Default._
          chat.send(Pickle.intoBytes[SharedMessages](Broadcast(name, messageField.value)).arrayBuffer())
          messageField.value = ""
          messageField.focus()
          event.preventDefault()
        }

      }

      chat.onerror = {e =>
        playground.insertBefore(p(s"failed : code ${e.asInstanceOf[ErrorEvent].colno}"), playground.firstChild)
        joinButton.disabled = false;
        sendButton.disabled = true;
      }
      chat.onclose = {e =>
        playground.insertBefore(p("连接已断开，请重新加入"), playground.firstChild)
        joinButton.disabled = false
        sendButton.disabled = true
      }
      chat.onmessage = {e =>
        var message = e.data
        message match {
          case buf: ArrayBuffer => {
            import boopickle.Default._
            Unpickle.apply[SharedMessages].fromBytes(TypedArrayBuffer.wrap(buf)) match {
              case Join(sender) =>
                playground.insertBefore(p(s"欢迎用户 ${sender} 登录！"), playground.firstChild)
              case Broadcast(sender, content) =>
                playground.insertBefore(p(s"${sender}:${content}"), playground.firstChild)
              case Leave(sender) =>
                playground.insertBefore(p(s"用户 ${sender} 退出"), playground.firstChild)
              case _ => {
                println("not supported 2" + TypedArrayBuffer.wrap(buf))
              }
            }
          }
          case _ => {
            println("not supported 1" + e.data)
          }
        }
        e
      }
    }

  def p(text: String) = {
    var p = dom.document.createElement("p").asInstanceOf[HTMLParagraphElement]
    p.innerHTML = text
    p
  }
}
