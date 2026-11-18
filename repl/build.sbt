name := "idml-repl"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-mtl"            % "1.7.0",
  "org.typelevel" %% "cats-effect"         % "3.7.1",
  "com.monovore"  %% "decline"             % "2.6.2",
  "com.lihaoyi"   %% "fansi"               % "0.2.13",
  "org.jline"      % "jline-terminal-jna"  % "3.13.3",
  "org.jline"      % "jline-reader"        % "3.13.3",
  "org.mockito"    % "mockito-all"         % "1.9.5" % Test,
  "org.scalatest" %% "scalatest"           % "3.2.19" % Test
)

Compile / mainClass := Some("io.idmlrepl.Main")
