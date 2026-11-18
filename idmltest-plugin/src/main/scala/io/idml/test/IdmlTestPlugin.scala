package io.idml.test

import java.nio.file.{Files, Path}

import sbt.*
import sbt.Keys.*
import cats.effect.{ExitCode, IO}
import cats.effect.unsafe.implicits.global
import cats.implicits.*

import scala.jdk.CollectionConverters.*
import scala.util.Using
import scala.util.control.NoStackTrace

object IdmlTestPlugin extends AutoPlugin {
  object autoImport {
    val idmlTest: TaskKey[Unit]             = taskKey[Unit]("run IDML tests")
    val idmlTestDirectory: SettingKey[File] = settingKey[File]("folder to run IDML tests in")
  }
  import autoImport.*

  override val trigger = allRequirements

  class TestRunFailed(s: String) extends Throwable(s) with NoStackTrace

  // trigger = allRequirements puts this on every project, so a project without an IDML test folder has no tests
  private def testFiles(folder: Path): IO[List[Path]] =
    IO.blocking {
      if (!Files.isDirectory(folder)) Nil
      else
        Using.resource(Files.list(folder))(
          _.iterator().asScala.filter(_.toString.endsWith(".json")).toList.sorted)
    }

  private def runAll(folder: Path): IO[TestState] = {
    val runner = new Runner(dynamic = true, plugins = None, jdiff = false)
    for {
      _       <- IO.println(s"Executing IDML tests in $folder")
      files   <- testFiles(folder)
      results <- files.flatTraverse(runner.runTest(false))
      _       <- runner.report(results)
    } yield results.combineAll
  }

  override val projectSettings: Seq[Setting[?]] = Seq(
    idmlTestDirectory := (Test / sourceDirectory).value / "idml",
    Test / test       := (Test / test).dependsOn(idmlTest).evaluated,
    Test / testFull   := (Test / testFull).dependsOn(idmlTest).value,
    idmlTest          := Def.uncached {
      val combined = runAll(idmlTestDirectory.value.toPath).unsafeRunSync()
      if (TestState.toExitCode(combined) == ExitCode.Error)
        throw new TestRunFailed(s"Test run failed, aggregate state is $combined")
    }
  )

}
