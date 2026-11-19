package io.idml.jsoup

import io.idml.IdmlValue
import org.jsoup.Jsoup
import org.jsoup.parser.{ParseSettings, Parser}

object IdmlJsoup {

  /** Parse an xml document. Tag and attribute names are lower-cased, as jsoup did before 1.9.
    */
  def parseXml(xml: String): IdmlValue =
    JsoupConverter(Jsoup.parse(xml, "", Parser.xmlParser().settings(ParseSettings.htmlDefault)))

  /** Parse a html document
    */
  def parseHtml(html: String): IdmlValue =
    JsoupConverter(Jsoup.parse(html, "", Parser.htmlParser()))

}
