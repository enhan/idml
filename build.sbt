enablePlugins(GitVersioning)
enablePlugins(JavaAppPackaging)
enablePlugins(DockerPlugin)

import sbtassembly.AssemblyPlugin.defaultShellScript

name := "idml-parent"

organization := "io.idml"

publishTo := localStaging.value

publishMavenStyle := true

isSnapshot := false

publishArtifact := false

licenses := Seq("MIT" -> uri("https://opensource.org/licenses/MIT"))

lazy val scala213 = "2.13.18"
lazy val scala3   = "3.3.9"

// Libraries cross-build; apps are Scala 3 only. `sbt compile` builds everything on Scala 3,
// `sbt +compile` / `sbt +testFull` adds the 2.13 build of the libraries.
ThisBuild / scalaVersion := scala3

// Versions shared by several modules
lazy val scalatestVersion            = "3.2.20"
lazy val scalatestplusMockitoVersion = "3.2.19.0"
lazy val circeVersion                = "0.14.17"
lazy val catsEffectVersion           = "3.7.1"
lazy val fs2Version                  = "3.16.0"
lazy val declineVersion              = "2.6.2"
lazy val attoVersion                 = "0.9.5"
lazy val fansiVersion                = "0.5.1"
lazy val jlineVersion                = "3.30.17"
lazy val http4sVersion               = "0.23.39"
// pins the jackson that json4s-jackson 4.0.7 brings (2.12.7: CVE-2022-42003/42004, CVE-2025-52999)
lazy val jacksonVersion              = "2.18.12"

// GitVersioning reads this at ThisBuild scope; release tags are plain numbers (2.1.1, not v2.1.1)
ThisBuild / git.gitTagToVersionNumber := { (tag: String) =>
  if (tag.matches("[0-9].*")) Some(tag) else None
}

lazy val commonSettings = Seq(
  organization            := "io.idml",
  crossScalaVersions      := List(scala3, scala213),
  isSnapshot              := false,
  publishArtifact         := true,
  publishTo               := localStaging.value,       // sbt 2 built-in Sonatype Central staging, see BUILD.md
  licenses                := Seq("MIT" -> uri("https://opensource.org/licenses/MIT")),
  homepage                := Some(uri("https://github.com/idml/idml")),
  scmInfo                 := Some(
    ScmInfo(uri("https://github.com/idml/idml"), "scm:git:git@github.com:idml/idml.git")),
  developers              := List(
    Developer(
      id = "andimiller",
      name = "Andi Miller",
      email = "andi@andimiller.net",
      url = uri("http://andimiller.net")),
    Developer(
      id = "teamrobin",
      name = "Team Robin",
      email = "teamrobin@meltwater.com",
      url = uri("https://meltwater.com"))
  ),
  scalacOptions ++= {
    if (scalaBinaryVersion.value == "3") Seq("-release:17", "-Ykind-projector")
    else Seq("-release:17", "-Xsource:3")
  },
  Docker / version        := version.value,
  Docker / dockerUsername := Some("idml"),
  dockerBaseImage         := "eclipse-temurin:17-jre", // pin: native-packager default follows the latest LTS
  libraryDependencies ++= {
    if (scalaBinaryVersion.value == "3") Nil
    else
      Seq(compilerPlugin(("org.typelevel" % "kind-projector" % "0.13.4").cross(CrossVersion.full)))
  },
  javacOptions ++= Seq("--release", "17"),
  exportJars              := false,                    // sbt 2 defaults to true; test harnesses read resources as plain files
  Test / fork             := true,
  Test / javaOptions ++= Seq(
    "-Duser.language=en",
    "-Duser.country=US"
  ) // tests assert English-formatted output
)

lazy val appSettings = commonSettings ++ Seq(
  crossScalaVersions := List(scala3)
)

// sbt 2 plugins compile with the Scala version sbt itself runs on (3.9.x for sbt 2.1), so they stay
// out of the 3.3/2.13 cross build: `+compile` / `+publishSigned` build them once, on that version.
lazy val sbtPluginSettings = commonSettings ++ Seq(
  scalaVersion       := appConfiguration.value.provider.scalaProvider.version,
  crossScalaVersions := List(scalaVersion.value),
  scalacOptions -= "-Ykind-projector", // deprecated on 3.9, and the plugins don't need it
  scriptedLaunchOpts += s"-Dplugin.version=${version.value}",
  scriptedBufferLog := false
)

// Explicit root: `crossScalaVersions := Nil` keeps it out of every `+` pass, so `+cmd` runs each module only at its
// own versions (an implicit root on 3.3.9 would re-run the aggregated plugins, and publish them twice).
lazy val root = Project("idml-root", file("."))
  .aggregate(
    lang,
    datanodes,
    jackson,
    circe,
    core,
    test,
    geo,
    jsoup,
    hashing,
    utils,
    repl,
    idmld,
    idmldoc,
    `idmldoc-plugin`,
    idmltest,
    `idmltest-plugin`,
    idmltutor,
    tool)
  .settings(
    crossScalaVersions := Nil,
    publish / skip     := true
  )

lazy val lang = project
  .settings(commonSettings)
  .settings(
    name := "idml-lang",
    libraryDependencies ++= Seq(
      "org.antlr"        % "antlr4-runtime" % "4.13.2", // BSD license, keep in sync with project/antlr4.sbt
      "com.google.guava" % "guava"          % "33.8.0-jre", // Apache License 2
      "org.scalatest"   %% "scalatest"      % scalatestVersion % Test // Apache License 2
    ),
    // Replaces sbt-antlr4 (no sbt 2 release): runs the ANTLR tool from the meta-build classpath.
    Compile / sourceGenerators += Def.task {
      val grammars = ((Compile / sourceDirectory).value / "antlr4" ** "*.g4").get().toSet
      val out      = (Compile / sourceManaged).value / "antlr4"
      val options  = Seq("-o", out.getPath, "-package", "io.idml.lang", "-listener", "-visitor")
      // cache keyed on tool version + options too, so changing either regenerates
      val cacheKey =
        s"antlr4-${org.antlr.v4.Tool.VERSION}-${Hash.toHex(Hash(options.mkString(" "))).take(12)}"
      val generate =
        FileFunction.cached(streams.value.cacheDirectory / cacheKey) { (in: Set[File]) =>
          IO.delete(out)
          val tool = new org.antlr.v4.Tool((options ++ in.map(_.getPath)).toArray)
          tool.processGrammarsOnCommandLine()
          if (tool.getNumErrors > 0) sys.error(s"ANTLR reported ${tool.getNumErrors} error(s)")
          (out ** "*.java").get().toSet
        }
      generate(grammars).toSeq
    }.taskValue
  )

lazy val datanodes = project
  .settings(commonSettings)
  .settings(
    name := "idml-datanodes",
    libraryDependencies ++= Seq(
      "joda-time"        % "joda-time"    % "2.15.1", // FIXME: Create date and time plugin
      "org.joda"         % "joda-convert" % "2.2.4", // FIXME: Create date and time plugin
      "com.sun.mail"     % "javax.mail"   % "1.6.2", // FIXME: Create email plugin
      "com.google.guava" % "guava"        % "33.8.0-jre",
      "org.typelevel"   %% "spire"        % "0.18.0",
      "org.typelevel"   %% "cats-core"    % "2.13.0",
      "com.google.re2j"  % "re2j"         % "1.8",
      "org.scalatest"   %% "scalatest"    % scalatestVersion % Test
    )
  )

lazy val jackson: Project = project
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "idml-jackson",
    libraryDependencies ++= List(
      "org.json4s"                %% "json4s-jackson"      % "4.0.7",
      "com.fasterxml.jackson.core" % "jackson-core"        % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind"    % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "org.scalatest"             %% "scalatest"           % scalatestVersion % Test
    )
  )

lazy val circe = project
  .dependsOn(datanodes)
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)
  .settings(
    name := "idml-circe",
    libraryDependencies ++= List(
      "io.circe"      %% "circe-core"    % circeVersion,
      "io.circe"      %% "circe-parser"  % circeVersion,
      "io.circe"      %% "circe-testing" % circeVersion     % "test",
      "org.scalatest" %% "scalatest"     % scalatestVersion % "test"
    )
  )

lazy val core: Project = project
  .dependsOn(datanodes)
  .dependsOn(lang)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
    name             := "idml-core",
    libraryDependencies ++= Seq(
      "org.slf4j"          % "slf4j-api"    % "1.7.36",
      "org.tpolecat"      %% "atto-core"    % attoVersion,
      "org.scalatest"     %% "scalatest"    % scalatestVersion            % Test,
      "org.scalatestplus" %% "mockito-5-18" % scalatestplusMockitoVersion % Test
    ),
    buildInfoKeys    := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion),
    buildInfoPackage := "io.idml",
    buildInfoOptions += BuildInfoOption.BuildTime
  )

lazy val test = project
  .dependsOn(core)
  .dependsOn(jackson % "compile->test")
  .dependsOn(circe % "test->compile")
  .settings(commonSettings)
  .settings(
    name            := "test",
    publishArtifact := false,
    libraryDependencies ++= List(
      "org.scalatest"     %% "scalatest"     % scalatestVersion            % Test,
      "org.scalatestplus" %% "mockito-5-18"  % scalatestplusMockitoVersion % Test,
      "io.circe"          %% "circe-testing" % circeVersion                % Test
    )
  )

lazy val geo = project
  .dependsOn(core)
  .settings(commonSettings)
  .dependsOn(jackson % "test->test")
  .dependsOn(test % "test->test")
  .settings(
    name           := "idml-geo",
    libraryDependencies ++= Seq(
      // "org.xerial"        % "sqlite-jdbc"    % "3.21.0.1",
      // "org.tpolecat"      %% "doobie-core"   % "0.6.0-M2",
      // "org.tpolecat"      %% "doobie-hikari" % "0.6.0-M2",
      "net.iakovlev"   % "timeshape" % "2026b.29",
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    ),
    Test / envVars := Map(
      "IDML_GEO_DB_DRIVER"       -> "org.sqlite.JDBC",
      "IDML_GEO_CITY_JDBC_URL"   -> "jdbc:sqlite::resource:cities.test.db",
      "IDML_GEO_ADMIN1_JDBC_URL" -> "jdbc:sqlite::resource:admin1.test.db"
    )
  )

lazy val jsoup = project
  .dependsOn(core)
  .dependsOn(test % "test->test")
  .settings(commonSettings)
  .settings(
    name := "idml-jsoup",
    libraryDependencies ++= Seq(
      "org.jsoup"      % "jsoup"     % "1.24.1",
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    )
  )

lazy val hashing = project
  .dependsOn(core)
  .settings(commonSettings)
  .settings(
    name := "idml-hashing",
    libraryDependencies ++= Seq(
      "net.openhft"    % "zero-allocation-hashing" % "2026.0",
      "at.yawk.lz4"    % "lz4-java"                % "1.14.1",
      "org.scalatest" %% "scalatest"               % scalatestVersion % Test
    )
  )

lazy val utils = project
  .dependsOn(core)
  .dependsOn(jsoup)
  .dependsOn(jackson % "test->test")
  .settings(commonSettings)
  .settings(
    name := "idml-utils",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % scalatestVersion % Test
    )
  )

lazy val repl = project
  .dependsOn(core)
  .dependsOn(jsoup)
  .dependsOn(hashing)
  .dependsOn(circe)
  .dependsOn(utils)
  .settings(appSettings)
  .settings(
    name                := "idml-repl",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-mtl"           % "1.7.0",
      "org.typelevel" %% "cats-effect"        % catsEffectVersion,
      "com.monovore"  %% "decline"            % declineVersion,
      "com.lihaoyi"   %% "fansi"              % fansiVersion,
      "org.jline"      % "jline-terminal-jna" % jlineVersion,
      "org.jline"      % "jline-reader"       % jlineVersion,
      "org.mockito"    % "mockito-all"        % "1.10.19"        % Test,
      "org.scalatest" %% "scalatest"          % scalatestVersion % Test
    ),
    Compile / mainClass := Some("io.idmlrepl.Main")
  )

lazy val idmld = project
  .dependsOn(core)
  .dependsOn(hashing)
  .dependsOn(jsoup)
  .dependsOn(utils)
  .dependsOn(circe)
  .dependsOn(geo)
  .settings(appSettings)
  .settings(
    name := "idmld",
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-core"         % http4sVersion,
      "org.http4s"    %% "http4s-dsl"          % http4sVersion,
      "org.http4s"    %% "http4s-circe"        % http4sVersion,
      "org.http4s"    %% "http4s-ember-server" % http4sVersion,
      "io.circe"      %% "circe-generic"       % circeVersion,
      "io.circe"      %% "circe-parser"        % circeVersion,
      "ch.qos.logback" % "logback-classic"     % "1.2.13",
      "org.scalatest" %% "scalatest"           % scalatestVersion % Test
    )
  )

lazy val idmldoc = project
  .dependsOn(core)
  .dependsOn(utils)
  .dependsOn(circe)
  .settings(appSettings)
  .settings(
    name                := "idmldoc",
    libraryDependencies ++= Seq(
      "org.tpolecat"  %% "atto-core"   % attoVersion,
      "com.lihaoyi"   %% "fastparse"   % "3.1.1",
      "co.fs2"        %% "fs2-io"      % fs2Version,
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "com.monovore"  %% "decline"     % declineVersion,
      "org.scalatest" %% "scalatest"   % scalatestVersion % Test
    ),
    Compile / mainClass := Some("io.idml.doc.Main")
  )

lazy val `idmldoc-plugin` = project
  .dependsOn(idmldoc)
  .enablePlugins(SbtPlugin)
  .settings(sbtPluginSettings)
  .settings(
    name := "idmldoc-plugin",
    addSbtPlugin("org.typelevel" % "laika-sbt" % "1.4.0-RC1")
  )

lazy val idmltest = project
  .dependsOn(core)
  .dependsOn(utils)
  .dependsOn(circe)
  .settings(appSettings)
  .settings(
    name                := "idml-test",
    libraryDependencies ++= Seq(
      "co.fs2"                        %% "fs2-io"        % fs2Version,
      "org.typelevel"                 %% "cats-effect"   % catsEffectVersion,
      "com.monovore"                  %% "decline"       % declineVersion,
      "io.circe"                      %% "circe-generic" % circeVersion,
      "io.circe"                      %% "circe-parser"  % circeVersion,
      "io.circe"                      %% "circe-literal" % circeVersion     % Test,
      "io.circe"                      %% "circe-yaml"    % "0.16.1",
      "com.lihaoyi"                   %% "fansi"         % fansiVersion,
      "org.gnieh"                     %% "diffson-circe" % "4.7.1",
      "org.tpolecat"                  %% "atto-core"     % attoVersion,
      "com.googlecode.java-diff-utils" % "diffutils"     % "1.3.0",
      "org.scalatest"                 %% "scalatest"     % scalatestVersion % Test
    ),
    Compile / mainClass := Some("io.idml.test.Main")
  )

lazy val `idmltest-plugin` = project
  .dependsOn(idmltest)
  .dependsOn(hashing)
  .dependsOn(geo)
  .dependsOn(jsoup)
  .dependsOn(jackson)
  .enablePlugins(SbtPlugin)
  .settings(sbtPluginSettings)
  .settings(
    name := "idmltest-plugin"
  )

lazy val idmltutor = project
  .dependsOn(hashing)
  .dependsOn(geo)
  .dependsOn(jsoup)
  .dependsOn(circe)
  .dependsOn(idmltest)
  .settings(appSettings)
  .settings(
    name                             := "idmltutor",
    libraryDependencies ++= List(
      "org.jline"     % "jline"         % jlineVersion,
      "org.tpolecat" %% "atto-core"     % attoVersion,
      "com.lihaoyi"  %% "fansi"         % fansiVersion,
      "io.circe"     %% "circe-literal" % circeVersion
    ),
    libraryDependencies ++= Seq(
      "org.mockito"    % "mockito-all" % "1.10.19"        % Test,
      "org.scalatest" %% "scalatest"   % scalatestVersion % Test
    ),
    Compile / mainClass              := Some("io.idml.tutor.Main"),
    dockerExposedPorts               := Seq(8081),
    Docker / packageName             := "idml",
    Docker / dockerUpdateLatest      := true,
    assembly / assemblyOption        := (assembly / assemblyOption).value.withPrependShellScript(
      Some(defaultShellScript)),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")                                    => MergeStrategy.discard
      case PathList("buildinfo/BuildInfo$.class")                                 => MergeStrategy.first
      case PathList("META-INF", "services", "io.idml.functions.FunctionResolver") =>
        MergeStrategy.concat
      case _                                                                      => MergeStrategy.first
    }
  )

lazy val tool = project
  .dependsOn(core)
  .dependsOn(jsoup)
  .dependsOn(utils)
  .dependsOn(repl)
  .dependsOn(idmld)
  .dependsOn(idmltest)
  .dependsOn(hashing)
  .dependsOn(idmltutor)
  .dependsOn(geo)
  .enablePlugins(DockerPlugin, JavaAppPackaging, UniversalPlugin)
  .settings(appSettings)
  .settings(
    name                             := "idml-tool",
    libraryDependencies ++= List(
      "com.monovore" %% "decline" % declineVersion
    ),
    Compile / mainClass              := Some("io.idml.tool.IdmlTool"),
    dockerExposedPorts               := Seq(8081),
    Docker / packageName             := "idml",
    Docker / dockerUpdateLatest      := true,
    assembly / assemblyOption        := (assembly / assemblyOption).value.withPrependShellScript(
      Some(defaultShellScript)),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "MANIFEST.MF")                                    => MergeStrategy.discard
      case PathList("buildinfo/BuildInfo$.class")                                 => MergeStrategy.first
      case PathList("META-INF", "services", "io.idml.functions.FunctionResolver") =>
        MergeStrategy.concat
      case _                                                                      => MergeStrategy.first
    }
  )

//lazy val geodb = project.dependsOn(core).dependsOn(geo)
