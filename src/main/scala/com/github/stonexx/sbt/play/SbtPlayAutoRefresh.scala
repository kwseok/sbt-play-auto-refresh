package com.github.stonexx.sbt.play

import play.sbt.PlayImport.PlayKeys
import play.sbt.{Play, PlayRunHook}
import sbt._
import sbt.Keys._
import unfiltered.netty._
import unfiltered.request.{GET, Path}
import unfiltered.response.{ComposeResponse, JsContent, ResponseString}

object SbtPlayAutoRefresh extends AutoPlugin {

  override def requires: Plugins = Play

  override def trigger: PluginTrigger = AllRequirements

  object autoImport {
    object PlayAutoRefreshKeys {
      val port = SettingKey[Int]("browser-notification-port")
    }
  }

  import autoImport.PlayAutoRefreshKeys._

  val webSockets = collection.mutable.Set.empty[websockets.WebSocket]

  val browserNotification = TaskKey[Unit]("browser-notification")

  val browserNotificationTask = Def.task[Unit] {
    webSockets.foreach(_.send("reload"))
  }

  override def projectSettings = Seq(
    port := 9999,
    browserNotification <<= browserNotificationTask.triggeredBy(compile in Compile),
    PlayKeys.playRunHooks += new BrowserNotifierPlayRunHook(port.value, state.value, streams.value)
  )

  class BrowserNotifierPlayRunHook(port: Int, state: State, streams: TaskStreams) extends PlayRunHook {
    import java.net.InetSocketAddress

    @io.netty.channel.ChannelHandler.Sharable
    object WSHandler extends websockets.Plan with websockets.CloseOnException {
      import websockets._

      def pass = DefaultPassHandler

      def intent = {
        case GET(Path("/")) => {
          case Open(s) => webSockets += s
          case Close(s) => webSockets -= s
          case Error(_, e) => streams.log.error(e.getMessage)
        }
      }
    }

    @io.netty.channel.ChannelHandler.Sharable
    object JSHandler extends cycle.Plan with cycle.SynchronousExecution with ServerErrorResponse {
      def intent = {
        case GET(Path("/play-auto-refresh.js")) =>
          new ComposeResponse(JsContent ~> ResponseString(
            s"""
               |(function(window){
               |  var Socket = "MozWebSocket" in window ? MozWebSocket : WebSocket;
               |  var ws = new Socket("ws://localhost:$port/");
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
    }

    lazy val server = Server.local(port).handler(JSHandler).handler(WSHandler)

    override def afterStarted(addr: InetSocketAddress): Unit = {
      server.start()
      streams.log.info(s"Started auto-refresh WebSocket on port $port")
    }

    override def afterStopped(): Unit = {
      server.stop()
      streams.log.info(s"Stopped auto-refresh WebSocket on port $port")
    }
  }

}
