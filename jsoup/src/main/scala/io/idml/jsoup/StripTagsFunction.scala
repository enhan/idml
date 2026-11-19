package io.idml.jsoup

import io.idml.datanodes.IString
import io.idml.{IdmlContext, InvalidCaller}
import io.idml.ast.{IdmlFunction, Pipeline}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.parser.Parser
import org.jsoup.safety.Safelist

object StripTagsFunction extends IdmlFunction {
  override def name: String = "stripTags"

  override def invoke(ctx: IdmlContext): Unit = {
    ctx.cursor = ctx.cursor match {
      case IString(str) =>
        IString(
          Parser.unescapeEntities(
            Jsoup.clean(str, "", Safelist.none(), new Document.OutputSettings().prettyPrint(false)),
            false
          )
        )
      case _            =>
        InvalidCaller
    }
  }

  override def args: List[Pipeline] = Nil
}
