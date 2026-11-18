package io.idml.doc

import java.nio.file.{Files, Path}

import cats.effect._
import cats._
import cats.implicits._
import com.monovore.decline._
import fs2._

import scala.jdk.CollectionConverters._

object Main extends IOApp {
  object CLI {
    case class Run(in: Path, out: Path)
    val in      = Opts.argument[Path]("input")
    val out     = Opts.argument[Path]("output")
    val program = Command(
      name = "idmldoc",
      header = "run IDML documentation from one folder into another"
    ) { (in, out).tupled }
  }

  def readFiles[F[_]: Sync](folder: Path): Stream[F, Path] =
    Stream.fromBlockingIterator[F](
      Files.walk(folder).filter(_.toString.endsWith(".md")).iterator().asScala,
      1)

  def processFile[F[_]: Sync: fs2.io.file.Files](in: Path, out: Path): F[Unit] =
    for {
      contents <- fs2.io.file
                    .Files[F]
                    .readAll(fs2.io.file.Path.fromNioPath(in))
                    .through(fs2.text.utf8.decode)
                    .compile
                    .foldMonoid
      _        <- Sync[F].blocking { Files.createDirectories(out.getParent) }
      _        <- Sync[F].delay { println(s"Compiling $in into $out") }
      parsed   <- Sync[F].delay { Markdown.parse(contents).get.value }
      ran      <- Runners.run[F](parsed)
      output   <- Sync[F].delay { Markdown.render(ran) }
      _        <- Stream
                    .emit(output)
                    .covary[F]
                    .through(fs2.text.utf8.encode)
                    .through(fs2.io.file.Files[F].writeAll(fs2.io.file.Path.fromNioPath(out)))
                    .compile
                    .drain
    } yield ()

  def processFiles[F[_]: Sync: fs2.io.file.Files](in: Path, out: Path): F[Unit] =
    readFiles[F](in)
      .evalMap { file =>
        processFile[F](file, out.resolve(in.relativize(file)))
      }
      .compile
      .drain

  override def run(args: List[String]): IO[ExitCode] =
    CLI.program
      .parse(args)
      .leftMap(h => IO { println(h); ExitCode.Error })
      .map { case (in, out) =>
        processFiles[IO](in, out).as(ExitCode.Success)
      }
      .bisequence
      .map(_.merge)
}
