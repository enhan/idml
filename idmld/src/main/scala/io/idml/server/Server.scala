package io.idml.server

import cats.effect._
import io.idml.FunctionResolverService
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object Server extends IOApp {
  override def run(args: List[String]): IO[ExitCode] =
    BlazeServerBuilder[IO](global)
      .withHttpApp(WebsocketServer.service(new FunctionResolverService).orNotFound)
      .bindHttp(8081, "localhost")
      .serve
      .compile
      .lastOrError
}
