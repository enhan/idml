name := "idml-test"

libraryDependencies ++= Seq(
  "co.fs2"                        %% "fs2-io"        % "3.15.0",
  "org.typelevel"                 %% "cats-effect"   % "3.7.1",
  "com.monovore"                  %% "decline"       % "2.6.2",
  "io.circe"                      %% "circe-generic" % "0.14.15",
  "io.circe"                      %% "circe-parser"  % "0.14.15",
  "io.circe"                      %% "circe-literal" % "0.14.15" % Test,
  "io.circe"                      %% "circe-yaml"    % "0.16.1",
  "com.lihaoyi"                   %% "fansi"         % "0.5.1",
  "org.gnieh"                     %% "diffson-circe" % "4.6.1",
  "org.tpolecat"                  %% "atto-core"     % "0.9.5",
  "com.googlecode.java-diff-utils" % "diffutils"     % "1.2.1"
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)

Compile / mainClass := Some("io.idml.test.Main")
