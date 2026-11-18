name := "test"

publishArtifact := false

libraryDependencies ++= List(
  "org.scalatest"     %% "scalatest"     % "3.2.19"   % Test,
  "org.scalatestplus" %% "mockito-5-18"  % "3.2.19.0" % Test,
  ("com.storm-enroute" %% "scalameter"    % "0.19"    % Test).exclude("org.scala-lang.modules", "scala-xml_2.13"), // scalameter wants scala-xml 1.x, scalatest 3.2.19 brings 2.x
  "io.circe"          %% "circe-testing" % "0.14.15" % Test,
  "org.scalatest"     %% "scalatest"     % "3.2.19"   % Test
)
