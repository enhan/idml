name := "idml-geo"

libraryDependencies ++= Seq(
//  "org.xerial"        % "sqlite-jdbc"    % "3.21.0.1",
//  "org.tpolecat"      %% "doobie-core"   % "0.6.0-M2",
//  "org.tpolecat"      %% "doobie-hikari" % "0.6.0-M2",
  "net.iakovlev"       % "timeshape"  % "2026b.29",
  "org.scalatest"     %% "scalatest"  % "3.2.19" % Test,
  ("com.storm-enroute" %% "scalameter" % "0.19"  % Test).exclude("org.scala-lang.modules", "scala-xml_2.13") // scalameter wants scala-xml 1.x, scalatest 3.2.19 brings 2.x
)
