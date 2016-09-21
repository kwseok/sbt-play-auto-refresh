sbtPlugin := true

organization := "com.github.stonexx.sbt"

name := "sbt-play-auto-refresh"

scalaVersion := "2.10.6"

resolvers ++= Seq(
  Resolver.typesafeRepo("releases"),
  Resolver.jcenterRepo
)

libraryDependencies ++= Seq(
  "net.databinder" %% "unfiltered-netty-server" % "0.8.4",
  "net.databinder" %% "unfiltered-netty-websockets" % "0.8.4",
  "net.databinder" %% "unfiltered-specs2" % "0.8.4" % "test"
)

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % sys.props.getOrElse("play.version", "2.5.8"))

licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
