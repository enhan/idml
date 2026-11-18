name := "idml-circe"

libraryDependencies ++= List(
  "io.circe"      %% "circe-core"    % "0.14.15",
  "io.circe"      %% "circe-parser"  % "0.14.15",
  "io.circe"      %% "circe-testing" % "0.14.15" % "test",
  "org.scalatest" %% "scalatest"     % "3.2.19"   % "test"
)
