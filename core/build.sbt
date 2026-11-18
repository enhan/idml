name := "idml-core"

libraryDependencies ++= Seq(
  "org.slf4j"          % "slf4j-api"   % "1.7.26",
  "org.tpolecat"      %% "atto-core"   % "0.9.5",
  "org.scalatest"     %% "scalatest"   % "3.2.19"   % Test,
  "org.scalatestplus" %% "mockito-5-18" % "3.2.19.0" % Test
)
