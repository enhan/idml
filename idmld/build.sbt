name := "idmld"

lazy val http4sVersion = "0.23.38"
lazy val circeVersion  = "0.14.15"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-core"         % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "org.http4s"    %% "http4s-ember-server" % http4sVersion,
  "io.circe"      %% "circe-generic"       % circeVersion,
  "io.circe"      %% "circe-parser"        % circeVersion,
  "ch.qos.logback" % "logback-classic"     % "1.0.1",
  "org.scalatest" %% "scalatest"           % "3.2.19" % Test
)
