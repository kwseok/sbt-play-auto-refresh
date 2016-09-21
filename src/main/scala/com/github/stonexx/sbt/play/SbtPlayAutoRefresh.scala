package com.github.stonexx.sbt.play

import play.sbt.PlayImport.PlayKeys
import play.sbt.{Play, PlayRunHook}
import sbt._
import sbt.Keys._
import unfiltered.netty._
import unfiltered.netty.websockets._
import unfiltered.request.{GET, Path}
import unfiltered.response.{ComposeResponse, JsContent, ResponseString}
import unfiltered.util.Port

object SbtPlayAutoRefresh extends AutoPlugin {

  override def requires: Plugins = Play

  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    object PlayAutoRefreshKeys {
      val wsPort = SettingKey[Int]("browser-notification-ws-port")
      val jsPort = SettingKey[Int]("browser-notification-js-port")
    }
  }

  import autoImport.PlayAutoRefreshKeys._

  val webSockets = collection.mutable.Set.empty[WebSocket]

  val browserNotification = TaskKey[Unit]("browser-notification")

  val browserNotificationTask = Def.task[Unit] {
    webSockets.foreach(_.send("reload"))
  }

  override def projectSettings = Seq(
    wsPort := Port.any,
    jsPort := 9999,
    browserNotification <<= browserNotificationTask.triggeredBy(compile in Compile),
    PlayKeys.playRunHooks += new BrowserNotifierPlayRunHook(wsPort.value, jsPort.value, state.value, streams.value)
  )

  class BrowserNotifierPlayRunHook(wsPort: Int, jsPort: Int, state: State, streams: TaskStreams) extends PlayRunHook {
    import java.net.InetSocketAddress

    lazy val wsServer = Server.local(wsPort).handler(
      Planify {
        case GET(Path("/")) => {
          case Open(s) => webSockets += s
          case Close(s) => webSockets -= s
          case Error(_, e) => streams.log.error(e.getMessage)
        }
      }
    )

    lazy val jsServer = Server.http(jsPort)
      .handler(new cycle.Plan with cycle.SynchronousExecution with ServerErrorResponse {
        def intent = {
          case GET(Path("/play-auto-refresh.js")) =>
            new ComposeResponse(JsContent ~> ResponseString(
              s"""
                 |(function(window){
                 |  var Socket = "MozWebSocket" in window ? MozWebSocket : WebSocket;
                 |  var ws = new Socket("ws://localhost:$wsPort/");
                 |  ws.onmessage = function(evt) {
                 |    switch (evt.data) {
                 |    case 'reload':
                 |      ws.close();
                 |      window.location.reload();
                 |      break;
                 |    }
                 |  };
                 |  window.__PLAY_AUTO_REFRESH_WS__ = ws
                 |})(window);
                 |""".stripMargin))
        }
      })

    override def afterStarted(addr: InetSocketAddress): Unit = {
      wsServer.start()
      jsServer.start()
      streams.log.info(s"Started auto-refresh WebSocket on port $wsPort")
    }

    override def afterStopped(): Unit = {
      wsServer.stop()
      jsServer.stop()
      streams.log.info(s"Stopped auto-refresh WebSocket on port $wsPort")
    }
  }

}
