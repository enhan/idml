name := "test"

publishArtifact := false

libraryDependencies ++= List(
  "org.scalatest"     %% "scalatest"     % "3.2.19"   % Test,
  "org.scalatestplus" %% "mockito-5-18"  % "3.2.19.0" % Test,
  "io.circe"          %% "circe-testing" % "0.14.15" % Test,
  "org.scalatest"     %% "scalatest"     % "3.2.19"   % Test
)
