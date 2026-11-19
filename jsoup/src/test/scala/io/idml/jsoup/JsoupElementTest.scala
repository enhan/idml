package io.idml.jsoup

import io.idml.MissingField
import io.idml.datanodes.{IDomElement, IDomText, IString}
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import scala.language.implicitConversions

class JsoupElementTest extends AnyFunSuite with Matchers {

  test("get(missing_tag) returns empty element") {
    IdmlJsoup
      .parseXml("<a>abc</a><b>def</b>")
      .get("c")
      .asInstanceOf[IDomElement]
      .items
      .toList must equal(List.empty)
  }

  test("get(nested_tag)[0] returns nested tag") {
    IdmlJsoup.parseXml("<a>abc</a><b>def</b>").get("b").get(0) must equal(
      IDomElement("b", Map.empty, List(IDomText("def"))))
  }

  test("get(deeply_nested_tag) returns missing empty array") {
    IdmlJsoup
      .parseXml("<a>abc<b>def</b></a>")
      .get("b")
      .asInstanceOf[IDomElement]
      .items
      .toList must equal(List.empty)
  }

  test("pull some text out") {
    IdmlJsoup.parseXml("<a>not me</a><b><c>hello</c><d>world</d></b>").get("b").text must equal(
      IString("helloworld"))
    IdmlJsoup.parseXml("<a>not me</a><b><c>hello</c><d>world</d></b>").get("a").text must equal(
      IString("not me"))
    IdmlJsoup.parseXml("<a>not me</a><b><c>hello</c><d>world</d></b>").text must equal(
      IString("not mehelloworld"))
  }

  test("parseXml lower-cases tag and attribute names") {
    // compare name/attrs directly: IdmlValue equality treats any two empty elements as equal
    IdmlJsoup.parseXml("<R><Mixed Case='Y'/></R>").get("r").get(0).get("mixed").get(0) match {
      case IDomElement(name, attrs, _) => (name, attrs) must equal(("mixed", Map("case" -> "Y")))
      case other                       => fail(s"expected an element, got $other")
    }
  }

}
