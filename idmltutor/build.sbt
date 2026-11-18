name := "idmltutor"

libraryDependencies ++= List(
  "org.jline"     % "jline"         % "3.13.3",
  "org.tpolecat" %% "atto-core"     % "0.9.5",
  "com.lihaoyi"  %% "fansi"         % "0.5.1",
  "io.circe"     %% "circe-literal" % "0.14.15"
)

libraryDependencies ++= Seq(
  "org.mockito"    % "mockito-all" % "1.9.5" % Test,
  "org.scalatest" %% "scalatest"   % "3.2.19" % Test
)

Compile / mainClass := Some("io.idml.tutor.Main")
