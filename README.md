# Play Auto Refresh
An play sbt plugin for the play auto-refresh.

Add plugin
----------

Add the plugin to `project/plugins.sbt`.

```scala
addSbtPlugin("com.github.stonexx.sbt" % "sbt-play-auto-refresh" % "1.0.1")
```

Your project's build file also needs to enable play sbt plugins. For example with build.sbt:

    lazy val root = (project.in file(".")).enablePlugins(PlayScala)

Add script tag to your webpage:
```html
<html>
<head>
<script src="http://localhost:9999/play-auto-refresh.js"></script>
</head>
<body>
</body>
</html>
```

Configuration
-------------

```scala
PlayAutoRefreshKeys.port := [number of port]
```
(if not set, defaults to 9999)

## License
`sbt-play-auto-refresh` is licensed under the [Apache License, Version 2.0](https://github.com/stonexx/sbt-play-auto-refresh/blob/master/LICENSE)
