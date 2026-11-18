package io.idml.doc

import sbt.*
import sbt.Keys.*
import laika.sbt.LaikaPlugin
import laika.sbt.LaikaPlugin.autoImport.*
import cats.effect.IO as CatsIO
import cats.effect.unsafe.implicits.global

/** Copies `idmlSourceDirectory` into `idmlTargetDirectory`, running the IDML code blocks of its
  * markdown on the way, and makes that the Laika input.
  */
object IdmlDocSbt extends AutoPlugin {

  override def requires = LaikaPlugin

  lazy val Idml               = config("Idml").extend(Compile).hide
  val makeIdml: TaskKey[Unit] =
    taskKey[Unit]("Compile the IDML docs and render them into a Laika site")
  val idml: TaskKey[Unit]     = taskKey[Unit]("Compile the IDML")

  val idmlTargetDirectory: SettingKey[File] =
    settingKey[File]("folder to put IDML docs in once they're built, added to Laika's sources")
  val idmlSourceDirectory: SettingKey[File] = settingKey[File]("folder to read IDML docs from")

  override def projectConfigurations: Seq[Configuration] = Seq(Idml)

  override val projectSettings: Seq[Setting[?]] = Seq(
    idmlSourceDirectory       := (Compile / sourceDirectory).value / "tut",
    idmlTargetDirectory       := (Compile / resourceManaged).value / "idml-docs",
    Laika / sourceDirectories := Seq(idmlTargetDirectory.value),
    idml                      := Def.uncached {
      val (in, out) = (idmlSourceDirectory.value, idmlTargetDirectory.value)
      IO.delete(out)
      IO.copyDirectory(in, out) // non-markdown assets (images, css) go through untouched
      Main.processFiles[CatsIO](in.toPath, out.toPath).unsafeRunSync()
    },
    laikaSite                 := laikaSite.dependsOn(idml).value,
    makeIdml                  := Def.uncached(laikaSite.map(_ => ()).value)
  )

}
