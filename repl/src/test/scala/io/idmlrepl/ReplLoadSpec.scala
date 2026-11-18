package io.idmlrepl

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import io.idml.circe.IdmlCirce
import org.scalatest.funsuite.AnyFunSuite

class ReplLoadSpec extends AnyFunSuite {
  test("decodeUtf8 strips a leading UTF-8 BOM so the file parses as JSON") {
    val file = Files.createTempFile("bom", ".json")
    try {
      Files.write(file, Array[Byte](0xef.toByte, 0xbb.toByte, 0xbf.toByte) ++ """{"a":1}""".getBytes(UTF_8))
      val decoded = Repl.decodeUtf8(Files.readAllBytes(file))
      assert(decoded == """{"a":1}""")
      assert(IdmlCirce.parseObjectEither(decoded).isRight)
    } finally Files.delete(file)
  }
}
