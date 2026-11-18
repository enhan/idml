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

// GitVersioning reads this at ThisBuild scope; release tags are plain numbers (2.1.1, not v2.1.1)
ThisBuild / git.gitTagToVersionNumber := { (tag: String) =>
  if (tag.matches("[0-9].*")) Some(tag) else None
}

lazy val commonSettings = Seq(
  organization              := "io.idml",
  crossScalaVersions        := List(scala3, scala213),
  isSnapshot                := false,
  publishArtifact           := true,
  publishTo                 := localStaging.value, // sbt 2 built-in Sonatype Central staging, see BUILD.md
  licenses                  := Seq("MIT" -> uri("https://opensource.org/licenses/MIT")),
  homepage                  := Some(uri("https://github.com/idml/idml")),
  scmInfo                   := Some(ScmInfo(uri("https://github.com/idml/idml"), "scm:git:git@github.com:idml/idml.git")),
  developers                := List(
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
  Docker / version          := version.value,
  Docker / dockerUsername   := Some("idml"),
  dockerBaseImage           := "eclipse-temurin:17-jre", // pin: native-packager default follows the latest LTS
  libraryDependencies ++= {
    if (scalaBinaryVersion.value == "3") Nil
    else Seq(compilerPlugin(("org.typelevel" % "kind-projector" % "0.13.4").cross(CrossVersion.full)))
  },
  javacOptions ++= Seq("--release", "17"),
  exportJars                := false, // sbt 2 defaults to true; test harnesses read resources as plain files
  Test / fork               := true,
  Test / javaOptions ++= Seq("-Duser.language=en", "-Duser.country=US") // tests assert English-formatted output
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
  scriptedBufferLog  := false
)

// Explicit root: `crossScalaVersions := Nil` keeps it out of every `+` pass, so `+cmd` runs each module only at its
// own versions (an implicit root on 3.3.9 would re-run the aggregated plugins, and publish them twice).
lazy val root = Project("idml-root", file("."))
  .aggregate(lang, datanodes, jackson, circe, core, test, geo, jsoup, hashing, utils, repl, idmld, idmldoc,
    `idmldoc-plugin`, idmltest, `idmltest-plugin`, idmltutor, tool)
  .settings(
    crossScalaVersions := Nil,
    publish / skip     := true
  )

lazy val lang = project
  .settings(commonSettings)

lazy val datanodes = project.settings(commonSettings)

lazy val jackson: Project =
  project.dependsOn(core % "compile->compile;test->test").settings(commonSettings)

lazy val circe = project
  .dependsOn(datanodes)
  .dependsOn(core % "compile->compile;test->test")
  .settings(commonSettings)

lazy val core: Project = project
  .dependsOn(datanodes)
  .dependsOn(lang)
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(
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
    publishArtifact := false
  )

lazy val geo = project
  .dependsOn(core)
  .settings(commonSettings)
  .dependsOn(jackson % "test->test")
  .dependsOn(test % "test->test")
  .settings(
    Test / envVars := Map(
      "IDML_GEO_DB_DRIVER"       -> "org.sqlite.JDBC",
      "IDML_GEO_CITY_JDBC_URL"   -> "jdbc:sqlite::resource:cities.test.db",
      "IDML_GEO_ADMIN1_JDBC_URL" -> "jdbc:sqlite::resource:admin1.test.db"
    )
  )

lazy val jsoup = project.dependsOn(core).dependsOn(test % "test->test").settings(commonSettings)

lazy val hashing = project.dependsOn(core).settings(commonSettings)

lazy val utils = project
  .dependsOn(core)
  .dependsOn(jsoup)
  .dependsOn(jackson % "test->test")
  .settings(commonSettings)

lazy val repl = project
  .dependsOn(core)
  .dependsOn(jsoup)
  .dependsOn(hashing)
  .dependsOn(circe)
  .dependsOn(utils)
  .settings(appSettings)

lazy val idmld =
  project
    .dependsOn(core)
    .dependsOn(hashing)
    .dependsOn(jsoup)
    .dependsOn(utils)
    .dependsOn(circe)
    .dependsOn(geo)
    .settings(appSettings)

lazy val idmldoc =
  project.dependsOn(core).dependsOn(utils).dependsOn(circe).settings(appSettings)

lazy val `idmldoc-plugin` = project
  .dependsOn(idmldoc)
  .enablePlugins(SbtPlugin)
  .settings(sbtPluginSettings)

lazy val idmltest =
  project.dependsOn(core).dependsOn(utils).dependsOn(circe).settings(appSettings)

lazy val `idmltest-plugin` = project
  .dependsOn(idmltest)
  .dependsOn(hashing)
  .dependsOn(geo)
  .dependsOn(jsoup)
  .dependsOn(jackson)
  .enablePlugins(SbtPlugin)
  .settings(sbtPluginSettings)

lazy val idmltutor = project
  .dependsOn(hashing)
  .dependsOn(geo)
  .dependsOn(jsoup)
  .dependsOn(circe)
  .dependsOn(idmltest)
  .settings(appSettings)
  .settings(
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
  .enablePlugins(DockerPlugin, JavaAppPackaging)
  .settings(appSettings)
  .settings(
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
