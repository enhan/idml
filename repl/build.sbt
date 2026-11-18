name := "idml-repl"

libraryDependencies ++= Seq(
  "co.fs2"        %% "fs2-io"              % "2.5.5",
  "org.typelevel" %% "cats-mtl-core"       % "0.7.1",
  "org.typelevel" %% "cats-tagless-macros" % "0.14.0",
  "org.typelevel" %% "cats-effect"         % "2.5.0",
  "com.monovore"  %% "decline"             % "1.4.0",
  "com.lihaoyi"   %% "fansi"               % "0.2.13",
  "org.jline"      % "jline-terminal-jna"  % "3.13.3",
  "org.jline"      % "jline-reader"        % "3.13.3",
  "org.mockito"    % "mockito-all"         % "1.9.5" % Test,
  "org.scalatest" %% "scalatest"           % "3.2.19" % Test
)

Compile / mainClass := Some("io.idmlrepl.Main")
