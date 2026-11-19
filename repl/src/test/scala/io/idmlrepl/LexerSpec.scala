package io.idmlrepl

import org.scalatest.funsuite.AnyFunSuite

class LexerSpec extends AnyFunSuite {
  test("the completion word is found by UTF-16 offset after non-BMP characters") {
    val line = "x = \"😀😀\" + root.ab"
    assert(Lexer(line, line.length).word() == "ab")
  }
}
