name := "idmld"

lazy val http4sVersion = "0.22.15" // last CE2 line; CE3 + http4s 0.23 comes in PR B
lazy val circeVersion  = "0.14.15"

libraryDependencies ++= Seq(
  "org.http4s"    %% "http4s-core"         % http4sVersion,
  "org.http4s"    %% "http4s-dsl"          % http4sVersion,
  "org.http4s"    %% "http4s-circe"        % http4sVersion,
  "org.http4s"    %% "http4s-blaze-server" % http4sVersion,
  "io.circe"      %% "circe-generic"       % circeVersion,
  "io.circe"      %% "circe-parser"        % circeVersion,
  "ch.qos.logback" % "logback-classic"     % "1.0.1"
)
