name := "idml-tool"

enablePlugins(UniversalPlugin)


libraryDependencies ++= List(
  "com.monovore" %% "decline" % "1.4.0",
)

Compile / mainClass := Some("io.idml.tool.IdmlTool")
