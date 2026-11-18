package io.idml.server

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import io.circe.Json
import io.circe.parser.parse
import io.idml.{FunctionResolverService, IdmlBuilder}
import org.http4s.{Method, Request, Status}
import org.http4s.implicits.*
import org.http4s.server.websocket.WebSocketBuilder2
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should

import scala.concurrent.duration.*

class DeepInputSpec extends AnyFlatSpec with should.Matchers {

  val fr   = new FunctionResolverService
  val idml = new IdmlBuilder(fr).build()

  // far past the nesting limit, yet well under MaxInputBytes
  val depth = 200000

  val deepIdml: String = "a = " + "(" * depth + "1" + ")" * depth

  def nestedJson(depth: Int): String = "{\"x\":" + "[" * depth + "]" * depth + "}"
  // as deep as the pre-scan allows once wrapped in {"in":[...]} by the requests below
  val deepJson: String               = nestedJson(BoundedStack.MaxNestingDepth - 3)
  val tooDeepJson: String            = nestedJson(depth)

  def wsRequest(in: String, mapping: String): String =
    Json
      .obj("in" -> parse(in).toOption.getOrElse(Json.Null), "idml" -> Json.fromString(mapping))
      .noSpaces

  def ws(msg: String): Json =
    parse(WebsocketServer.handleText(idml)(msg).unsafeRunSync()).toOption.getOrElse(Json.Null)

  def errors(response: Json): Option[List[String]] =
    response.hcursor.get[List[String]]("errors").toOption

  def completion(body: String): (Status, String) =
    WebSocketBuilder2[IO]
      .flatMap { wsb =>
        WebsocketServer
          .service(fr)(wsb)
          .orNotFound
          .run(Request[IO](Method.POST, uri"/completion").withEntity(body))
      }
      .flatMap(r => r.bodyText.compile.string.map(r.status -> _))
      .unsafeRunSync()

  "the websocket handler" should "answer deeply nested IDML with an error" in {
    errors(ws(wsRequest("[{}]", deepIdml))) shouldBe Some(BoundedStack.TooDeep)
  }

  it should "reject IDML, paths and JSON just past the nesting limit before running them" in {
    val tooDeep = BoundedStack.MaxNestingDepth + 1
    errors(ws(wsRequest("[{}]", "a = " + "(" * tooDeep + "1" + ")" * tooDeep))) shouldBe Some(
      BoundedStack.TooDeep)
    val pathMsg = Json.obj(
      "in"   -> Json.arr(Json.obj()),
      "idml" -> Json.fromString("a = 1"),
      "path" -> Json.fromString("(" * tooDeep + "a" + ")" * tooDeep))
    errors(ws(pathMsg.noSpaces)) shouldBe Some(BoundedStack.TooDeep)
    errors(ws(s"""{"in":[${nestedJson(tooDeep)}],"idml":"a = 1"}""")) shouldBe Some(
      BoundedStack.TooDeep)
  }

  it should "not count brackets inside JSON strings towards the nesting limit" in {
    val brackets = "[" * (BoundedStack.MaxNestingDepth + 1)
    errors(ws(wsRequest(s"""[{"s":"$brackets"}]""", "b = s"))) shouldBe None
  }

  it should "run mappings on JSON documents nested up to the nesting limit" in {
    val response = ws(s"""{"in":[$deepJson],"idml":"a = 1"}""")
    errors(response) shouldBe None
    response.hcursor.downField("out").as[List[Json]].toOption shouldBe Some(
      List(Json.obj("result" -> Json.obj("a" -> Json.fromInt(1)))))
  }

  it should "answer JSON documents nested beyond the nesting limit with an error" in {
    errors(ws(s"""{"in":[$tooDeepJson],"idml":"a = 1"}""")) shouldBe Some(BoundedStack.TooDeep)
  }

  it should "reject oversized messages" in {
    errors(ws(" " * (BoundedStack.MaxInputBytes + 1))) shouldBe Some(BoundedStack.TooLarge)
  }

  it should "count multi-byte characters when rejecting oversized messages" in {
    errors(ws("é" * (BoundedStack.MaxInputBytes / 2 + 1))) shouldBe Some(BoundedStack.TooLarge)
  }

  it should "still run normal mappings afterwards" in {
    val response = ws(wsRequest("""[{"a":1}]""", "b = a"))
    errors(response) shouldBe None
    response.hcursor.downField("out").as[List[Json]].toOption shouldBe Some(
      List(Json.obj("result" -> Json.obj("b" -> Json.fromInt(1)))))
  }

  "the completion endpoint" should "complete on JSON documents nested up to the nesting limit" in {
    val (status, body) = completion(s"""{"in":[$deepJson],"idml":"result = root.","position":14}""")
    status shouldBe Status.Ok
    parse(body).flatMap(_.as[List[String]]).toOption shouldBe Some(List("x"))
  }

  it should "answer malformed IDML with a bad request" in {
    val (status, body) =
      completion("""{"in":[{"a":1}],"idml":"result = ((root.","position":16}""")
    status shouldBe Status.BadRequest
    parse(body).toOption.flatMap(errors).map(_.nonEmpty) shouldBe Some(true)
  }

  it should "answer JSON nested beyond the nesting limit with a bad request" in {
    val (status, _) =
      completion(s"""{"in":[$tooDeepJson],"idml":"result = root.","position":14}""")
    status shouldBe Status.BadRequest
  }

  it should "reject oversized bodies" in {
    val (status, body) = completion(" " * (BoundedStack.MaxInputBytes + 1))
    status shouldBe Status.BadRequest
    body should include(BoundedStack.TooLarge.head)
  }

  it should "still complete normal requests afterwards" in {
    val (status, body) =
      completion("""{"in":[{"a":1,"b":2}],"idml":"result = root.","position":14}""")
    status shouldBe Status.Ok
    parse(body).flatMap(_.as[List[String]]).toOption.map(_.toSet) shouldBe Some(Set("a", "b"))
  }

  "BoundedStack.run" should "keep serving after a class initialiser overflowed" in {
    def poisoned = BoundedStack.run("{}")(_ => Right(PoisonedOnInit.value)).unsafeRunSync()
    poisoned shouldBe Left(BoundedStack.TooDeep)  // the overflow inside <clinit>
    poisoned shouldBe Left(BoundedStack.Internal) // then NoClassDefFoundError, a LinkageError
    errors(ws(wsRequest("""[{"a":1}]""", "b = a"))) shouldBe None
  }

  it should "answer with an error when the work takes longer than the timeout" in {
    BoundedStack
      .run("{}", 10.millis) { _ => Thread.sleep(500); Right(()) }
      .unsafeRunSync() shouldBe Left(BoundedStack.TimedOut)
  }

  "the warm-up requests" should "succeed" in {
    errors(ws(WebsocketServer.WarmUpMapping)) shouldBe None
    completion(WebsocketServer.WarmUpCompletion)._1 shouldBe Status.Ok
    noException should be thrownBy WebsocketServer.warmUp(fr).unsafeRunSync()
  }
}

/** Overflows its stack while being initialised, which leaves the class unusable afterwards. */
object PoisonedOnInit {
  private def deep(n: Int): Int = if (n == Int.MinValue) 0 else deep(n + 1) + 1
  val value: Int                = deep(0)
}
