name := "idmldoc"

libraryDependencies ++= Seq(
  "org.tpolecat"  %% "atto-core"   % "0.9.5",
  "com.lihaoyi"   %% "fastparse"   % "3.1.1",
  "co.fs2"        %% "fs2-io"      % "3.15.0",
  "org.typelevel" %% "cats-effect" % "3.7.1",
  "com.monovore"  %% "decline"     % "2.6.2"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

Compile / mainClass := Some("io.idml.doc.Main")
