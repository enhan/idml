package io.idml.utils.configuration

import io.idml.{Idml, IdmlBuilder, IdmlChain, Mapping}
import atto._
import Atto._
import cats._
import cats.data._
import cats.implicits._

/** A small DSL that can be used to combine mappings, examples look like this: a|b # a piped into b
  * a+b # a and b run on the same input then merged
  *
  * Note: | takes precedence over + so in `a|b+c` the `b` and `c` mappings are combined, and fed the
  * output from `a`
  */
object Pipeline {
  sealed trait ConfigurationMapping
  object ConfigurationMapping {
    final case class SingleMapping(name: String) extends ConfigurationMapping
    final case class Merged(lhs: ConfigurationMapping, rhs: ConfigurationMapping)
        extends ConfigurationMapping
    final case class Chained(lhs: ConfigurationMapping, rhs: ConfigurationMapping)
        extends ConfigurationMapping

  }
  import ConfigurationMapping._

  object Lexer {
    sealed trait Token
    case class Text(value: String) extends Token
    case object Pipe               extends Token
    case object Plus               extends Token

    val chars = Set('+', '|')
    val text  = takeWhile1(!chars.contains(_)).map(Text.apply).widen[Token]
    val pipe  = char('|').as(Pipe).widen[Token]
    val plus  = char('+').as(Plus).widen[Token]

    val lexer                                                                                  = many1(text | (pipe | plus))
    def lex[M[_]](s: String)(implicit AE: ApplicativeError[M, String]): M[NonEmptyList[Token]] =
      AE.fromEither(lexer.parseOnly(s).either)
  }

  object Parser {
    class SplitAtFirstX[X](x: X) {
      def unapply(xs: List[X]): Option[(List[X], List[X])] = {
        val idx = xs.indexOf(x)
        if (idx != -1) {
          val (a, b) = xs.splitAt(idx)
          Some((a, b.drop(1)))
        } else {
          None
        }
      }
    }
    object SplitAtFirstPipe extends SplitAtFirstX[Lexer.Token](Lexer.Pipe)
    object SplitAtFirstPlus extends SplitAtFirstX[Lexer.Token](Lexer.Plus)

    def parse[M[_]: Monad](tokens: NonEmptyList[Lexer.Token])(implicit
        AE: ApplicativeError[M, String]): M[ConfigurationMapping] =
      parseTokens[M](tokens.toList)

    // | splits before +, so `a|b+c` is Chained(a, Merged(b, c))
    private def parseTokens[M[_]](tokens: List[Lexer.Token])(implicit
        AE: ApplicativeError[M, String]): M[ConfigurationMapping] = {
      val mode = tokens.find(_ == Lexer.Pipe).orElse(tokens.find(_ == Lexer.Plus))
      (mode, tokens) match {
        case (None, Lexer.Text(t) :: Nil)               => AE.pure(SingleMapping(t))
        case (Some(Lexer.Pipe), SplitAtFirstPipe(l, r)) =>
          AE.map2(parseTokens[M](l), parseTokens[M](r))(Chained(_, _))
        case (Some(Lexer.Plus), SplitAtFirstPlus(l, r)) =>
          AE.map2(parseTokens[M](l), parseTokens[M](r))(Merged(_, _))
        case _                                          => AE.raiseError(s"Parse error, unexpected tokens: $tokens")
      }
    }

    def apply[M[_]](s: String)(implicit ME: MonadError[M, String]): M[ConfigurationMapping] =
      for {
        tokens <- Lexer.lex[M](s)
        ast    <- Parser.parse[M](tokens)
      } yield ast
  }

  def combine[M[_]: Monad](f: String => M[Mapping])(m: ConfigurationMapping): M[Mapping] =
    m match {
      case SingleMapping(value) => f(value)
      case Merged(l, r)         =>
        (combine(f)(l), combine(f)(r)).mapN((a, b) => Mapping.fromMultipleMappings(List(a, b)))
      case Chained(l, r)        => (combine(f)(l), combine(f)(r)).mapN(new IdmlChain(_, _))
    }

  def print(m: ConfigurationMapping): Eval[String] =
    m match {
      case SingleMapping(value) => Eval.later(value)
      case Merged(l, r)         => (print(l), print(r)).mapN((a, b) => s"($a + $b)")
      case Chained(l, r)        => (print(l), print(r)).mapN((a, b) => s"($a | $b)")
    }

  def run[M[_]](f: String => M[Mapping])(implicit ME: MonadError[M, String]): String => M[Mapping] =
    (s: String) =>
      for {
        tokens   <- Lexer.lex[M](s)
        ast      <- Parser.parse[M](tokens)
        combined <- combine[M](f)(ast)
      } yield combined

}
