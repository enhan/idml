name := "idml-jsoup"

libraryDependencies ++= Seq(
  "org.jsoup"          % "jsoup"      % "1.8.1",
  "org.scalatest"     %% "scalatest"  % "3.2.19" % Test,
  ("com.storm-enroute" %% "scalameter" % "0.19"  % Test).exclude("org.scala-lang.modules", "scala-xml_2.13") // scalameter wants scala-xml 1.x, scalatest 3.2.19 brings 2.x
)
