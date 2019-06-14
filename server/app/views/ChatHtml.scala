package views

import controllers.routes

object ChatHtml {
  def getHtml =
  {
    var js = scalajs.html.scripts("client", routes.Assets.versioned(_).toString, name => getClass.getResource(s"/public/$name") != null)
    "<!DOCTYPE html>" +
      s"""<html>
            <body>
              <div>聊天室</div>

              <form action="#">
                <label for="name">姓名: </label><input id="name" type="text" />
                <input id="join" type="button" value="加入!"/>
              </form>

              <form action="#">
                <label for="message">说点什么: </label><input id="message" type="text" />
                <input id="send" type="button" value="发送" disabled="true"/>
              </form>

              <div id="playground"></div>
              ${js}
            </body>
          </html>"""
  }

}
