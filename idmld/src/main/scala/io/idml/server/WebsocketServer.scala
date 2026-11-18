package io.idml.server

import cats.effect.IO
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe._
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import cats._
import cats.syntax._
import cats.implicits._
import cats.data._
import cats.effect._
import io.idml.utils.{AnalysisModule, AutoComplete}
import io.idml.ast.IdmlFunctionMetadata
import io.idml.hashing.HashingFunctionResolver
import io.idml.jsoup.JsoupFunctionResolver
import io.idml.geo.GeoFunctionResolver
import io.idml._
import io.idml.circe.instances._
import io.idml.circe._
import fs2._
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame.Text
import org.slf4j.LoggerFactory

import scala.jdk.CollectionConverters._
import scala.util.Try

object WebsocketServer {
  val log = LoggerFactory.getLogger(getClass)

  import org.http4s.dsl.io._
  import org.http4s.websocket._
  import org.http4s.circe.CirceEntityDecoder._

  case class Request(in: List[IdmlObject], idml: String, path: Option[String])
  case class Response(out: Option[List[Json]], errors: Option[List[String]])

  case class Complete(in: List[IdmlObject], idml: String, position: Int)

  val functions =
    (StaticFunctionResolverService.defaults(IdmlCirce).asScala ++ List(
      new JsoupFunctionResolver,
      new HashingFunctionResolver,
      new GeoFunctionResolver(IdmlCirce)
    )).toList
      .flatMap { f =>
        f.providedFunctions().filterNot(_.name.startsWith("$"))
      }

  val completionIdml =
    Idml
      .staticBuilderWithDefaults(IdmlCirce)
      .withResolver(new JsoupFunctionResolver)
      .withResolver(new HashingFunctionResolver)
      .withResolver(new AnalysisModule)
      .withResolver(new GeoFunctionResolver(IdmlCirce))
      .build()

  def service(fr: FunctionResolverService)(wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = {
    val idml = new IdmlBuilder(fr).build()
    HttpRoutes.of[IO] {
      case GET -> Root / "functions" / partial =>
        Ok(functions.filter(_.name.startsWith(partial)).asJson)
      case req @ POST -> Root / "completion"   =>
        readCapped(req)
          .flatMap(_.fold(e => IO.pure(Left(e)), BoundedStack.run(_)(complete)))
          .flatMap(_.fold(e => BadRequest(Response(None, Some(e)).asJson), Ok(_)))

      case GET -> Root =>
        val main: Pipe[IO, WebSocketFrame, String] = _.evalMap {
          case Text(msg, _) => handleText(idml)(msg)
          case _            => IO.pure(render(Left(List("Please send your request as Text"))))
        }

        wsb
          .withMaxMessageSize(BoundedStack.MaxInputBytes.toLong)
          .build(main.andThen(_.map(Text(_))))

    }
  }

  /** Answers one websocket text message; never fails, errors are part of the rendered response. */
  def handleText(idml: Idml)(msg: String): IO[String] =
    BoundedStack
      .run(msg)(m => Right(render(evaluate(idml)(m))))
      .map(_.fold(e => render(Left(e)), identity))

  /** Runs one small mapping and completion through the request path at startup, so that common
    * classes get initialised on a shallow stack rather than first inside a hostile deep request.
    */
  def warmUp(fr: FunctionResolverService): IO[Unit] =
    for {
      mapped    <- handleText(new IdmlBuilder(fr).build())(WarmUpMapping)
      completed <- BoundedStack.run(WarmUpCompletion)(complete)
    } yield log.info(s"Warm-up done: $mapped ${completed.fold(_.mkString, _.noSpaces)}")

  private[server] val WarmUpMapping =
    """{"in":[{"a":"x","n":[1,2]}],"idml":"b = a.uppercase()\nc = n.map(this + 1)\nd = {\"k\": a}","path":"root"}"""

  private[server] val WarmUpCompletion =
    """{"in":[{"a":1}],"idml":"result = root.","position":14}"""

  private def render(result: Either[List[String], List[Json]]): String =
    result
      .fold(e => Response(None, Some(e)), out => Response(Some(out), None))
      .asJson
      .toString()

  private def evaluate(idml: Idml)(msg: String): Either[List[String], List[Json]] =
    _root_.io.circe.parser
      .decode[Request](msg)
      .leftMap(e => List(e.getMessage))
      .filterOrElse(x => !(x.idml :: x.path.toList).exists(tooDeep), BoundedStack.TooDeep)
      .flatMap { x =>
        Try {
          log.debug("Request: {}", x)
          val chain = idml.compile(x.idml)
          val p     = x.path.getOrElse("root")
          val path  = idml.compile(s"result = $p")
          x.in.map { j =>
            Try { path.run(chain.run(j)).asJson }.toEither
          }.sequence
        }.toEither
          .leftMap(e => List(e.getMessage))
          .flatMap(_.leftMap(e => List(e.getMessage)))
      }

  private def complete(body: String): Either[List[String], Json] =
    _root_.io.circe.parser
      .decode[Complete](body)
      .leftMap(e => List(e.getMessage))
      .filterOrElse(c => !tooDeep(c.idml), BoundedStack.TooDeep)
      .flatMap { c =>
        Try {
          c.in.flatMap { doc =>
            AutoComplete.complete(completionIdml)(doc, c.idml, c.position)
          }.asJson
        }.toEither.leftMap(e => List(e.getMessage))
      }

  private def tooDeep(idml: String): Boolean =
    BoundedStack.idmlNestingDepth(idml) > BoundedStack.MaxNestingDepth

  // reads at most one byte past the limit, so an oversized body is never fully buffered
  private def readCapped(req: org.http4s.Request[IO]): IO[Either[List[String], String]] =
    req.body
      .take(BoundedStack.MaxInputBytes.toLong + 1)
      .compile
      .to(Array)
      .map { bytes =>
        Either.cond(
          bytes.length <= BoundedStack.MaxInputBytes,
          new String(bytes, java.nio.charset.StandardCharsets.UTF_8),
          BoundedStack.TooLarge)
      }

}
