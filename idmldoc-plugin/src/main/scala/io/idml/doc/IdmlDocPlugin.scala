package io.idml.doc

import java.nio.file.Path

import sbt.*
import sbt.Keys.*
import laika.sbt.LaikaPlugin
import laika.sbt.LaikaPlugin.autoImport.*
import cats.effect.IO as CatsIO
import cats.effect.unsafe.IORuntime

import scala.util.Try

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

  private def canonical(f: File): Either[String, Path] =
    // unsafe: getCanonicalFile can throw on I/O errors, reported as a task failure
    Try(f.getCanonicalFile.toPath).toEither.left.map(e => s"cannot resolve $f: ${e.getMessage}")

  /** `idml` deletes `out` before copying `in` into it, so `out` must not overlap `in` or contain
    * the project.
    */
  private[doc] def checkTarget(in: File, out: File, base: File): Either[String, Unit] =
    for {
      i <- canonical(in)
      o <- canonical(out)
      b <- canonical(base)
      _ <-
        if (i.startsWith(o))
          Left(s"idmlTargetDirectory ($o) is or contains idmlSourceDirectory ($i)")
        else if (o.startsWith(i))
          Left(s"idmlTargetDirectory ($o) is inside idmlSourceDirectory ($i)")
        else if (b.startsWith(o))
          Left(s"idmlTargetDirectory ($o) is or contains the project directory ($b)")
        else Right(())
    } yield ()

  private def runSync[A](io: CatsIO[A]): A = {
    // a runtime per run: the global one would keep its threads, and this classloader, alive across reloads
    val runtime = IORuntime.builder().build()
    try io.unsafeRunSync()(runtime)
    finally runtime.shutdown()
  }

  override val projectSettings: Seq[Setting[?]] = Seq(
    idmlSourceDirectory       := (Compile / sourceDirectory).value / "tut",
    idmlTargetDirectory       := (Compile / resourceManaged).value / "idml-docs",
    Laika / sourceDirectories := Seq(idmlTargetDirectory.value),
    idml                      := Def.uncached {
      val (in, out) = (idmlSourceDirectory.value, idmlTargetDirectory.value)
      checkTarget(in, out, baseDirectory.value)
        .fold(msg => throw new MessageOnlyException(msg), identity)
      IO.delete(out)
      IO.copyDirectory(in, out) // non-markdown assets (images, css) go through untouched
      runSync(Main.processFiles[CatsIO](in.toPath, out.toPath))
    },
    // every Laika task that reads the input; laikaPackageSite goes through laikaSite
    laikaSite                 := laikaSite.dependsOn(idml).value,
    laikaGenerate             := laikaGenerate.dependsOn(idml).evaluated,
    laikaHTML                 := laikaHTML.dependsOn(idml).value,
    laikaXSLFO                := laikaXSLFO.dependsOn(idml).value,
    laikaEPUB                 := laikaEPUB.dependsOn(idml).value,
    laikaPDF                  := laikaPDF.dependsOn(idml).value,
    laikaAST                  := laikaAST.dependsOn(idml).value,
    laikaDescribe             := laikaDescribe.dependsOn(idml).value,
    laikaPreview              := laikaPreview.dependsOn(idml).value, // once, at server start
    makeIdml                  := Def.uncached(laikaSite.map(_ => ()).value)
  )

}
