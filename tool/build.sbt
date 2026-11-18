name := "idml-tool"

enablePlugins(UniversalPlugin)


libraryDependencies ++= List(
  "com.monovore" %% "decline" % "2.6.2",
)

Compile / mainClass := Some("io.idml.tool.IdmlTool")
