package io.idml.server

import cats.effect._
import com.comcast.ip4s._
import io.idml.FunctionResolverService
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits._

object Server extends IOApp {
  def serve(host: Host, fr: FunctionResolverService): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(host)
      .withPort(port"8081")
      .withHttpWebSocketApp(WebsocketServer.service(fr)(_).orNotFound)
      .build
      .useForever

  override def run(args: List[String]): IO[ExitCode] =
    serve(host"localhost", new FunctionResolverService)
}
