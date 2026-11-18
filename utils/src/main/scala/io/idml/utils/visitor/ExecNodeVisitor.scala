// scalastyle:off number.of.methods
package io.idml.utils.visitor

import io.idml.ast._
import io.idml.functions.ApplyFunction

/** Implements the visitor pattern on top of the Document hierarchy */
trait ExecNodeVisitor {
  this: VisitationStyle =>

  trait ExecNodeContext
  case class ExecDocContext(doc: Document)                          extends ExecNodeContext
  case class ExecBlockContext(block: Block, parent: ExecDocContext) extends ExecNodeContext

  trait ExecRuleContext                                                    extends ExecNodeContext {
    def rule: Rule; def parent: ExecBlockContext
  }
  case class ExecAssignContext(rule: Assignment, parent: ExecBlockContext) extends ExecRuleContext
  case class ExecReassignContext(rule: Reassignment, parent: ExecBlockContext)
      extends ExecRuleContext
  case class ExecVariableContext(rule: Variable, parent: ExecBlockContext) extends ExecRuleContext

  case class ExecPiplContext(pipl: Pipeline, parent: ExecRuleContext) extends ExecNodeContext

  trait ExecExprContext                                                   extends ExecNodeContext {
    def expr: Expression
    def parent: ExecPiplContext
  }
  case class ExecAnyContext(parent: ExecPiplContext)                      extends ExecExprContext {
    def expr: AnyExpr.type = AnyExpr
  }
  case class ExecIndexContext(expr: Index, parent: ExecPiplContext)       extends ExecExprContext
  case class ExecNavContext(expr: ExecNav, parent: ExecPiplContext)       extends ExecExprContext
  case class ExecPathContext(expr: Field, parent: ExecPiplContext)        extends ExecExprContext
  case class ExecSliceContext(expr: Slice, parent: ExecPiplContext)       extends ExecExprContext
  case class ExecBlockInvokeContext(expr: ApplyFunction, parent: ExecPiplContext)
      extends ExecExprContext
  case class ExecFuncContext(expr: IdmlFunction, parent: ExecPiplContext) extends ExecExprContext
  case class ExecCoalesceContext(expr: Coalesce, parent: ExecPiplContext) extends ExecExprContext
  case class ExecWildcardContext(expr: Wildcard, parent: ExecPiplContext) extends ExecExprContext
  case class ExecFilterContext(expr: Filter, parent: ExecPiplContext)     extends ExecExprContext
  case class ExecMathsContext(expr: Maths, parent: ExecPiplContext)       extends ExecExprContext
  case class ExecIfContext(expr: If, parent: ExecPiplContext)             extends ExecExprContext
  case class ExecMatchContext(expr: Match, parent: ExecPiplContext)       extends ExecExprContext
  case class ExecArrayContext(expr: AstArray, parent: ExecPiplContext)    extends ExecExprContext
  case class ExecObjectContext(expr: AstObject, parent: ExecPiplContext)  extends ExecExprContext

  def visit(doc: Document): Unit = {
    visitDoc(ExecDocContext(doc))
  }

  def visitBlock(ctx: ExecBlockContext): Unit = {
    ctx.block.rules foreach { rule =>
      visitRule(createRuleContext(rule, ctx))
    }
  }

  def visitRule(ctx: ExecRuleContext): Unit = {
    ctx match {
      case ctx: ExecAssignContext   => visitAssign(ctx)
      case ctx: ExecReassignContext => visitReassign(ctx)
      case ctx: ExecVariableContext => visitVariable(ctx)
    }
  }

  def createRuleContext(rule: Rule, ctx: ExecBlockContext): ExecRuleContext = {
    rule match {
      case rule: Assignment   => ExecAssignContext(rule, ctx)
      case rule: Reassignment => ExecReassignContext(rule, ctx)
      case rule: Variable     => ExecVariableContext(rule, ctx)
    }
  }

  def visitAssign(ctx: ExecAssignContext): Unit = {
    visitPipl(ExecPiplContext(ctx.rule.exps, ctx))
  }

  def visitReassign(ctx: ExecReassignContext): Unit = {
    visitPipl(ExecPiplContext(ctx.rule.exps, ctx))
  }

  def visitVariable(ctx: ExecVariableContext): Unit = {
    visitPipl(ExecPiplContext(ctx.rule.exps, ctx))
  }

  def visitPipl(ctx: ExecPiplContext): Unit = {
    ctx.pipl.exps foreach { expr =>
      visitExpr(createExprContext(expr, ctx))
    }
  }

  def createExprContext(expr: Expression, ctx: ExecPiplContext): ExecExprContext = {
    expr match {
      case AnyExpr             => ExecAnyContext(ctx)
      case expr: Index         => ExecIndexContext(expr, ctx)
      case expr: ExecNav       => ExecNavContext(expr, ctx)
      case expr: Field         => ExecPathContext(expr, ctx)
      case expr: Slice         => ExecSliceContext(expr, ctx)
      case expr: ApplyFunction => ExecBlockInvokeContext(expr, ctx)
      case expr: IdmlFunction  => ExecFuncContext(expr, ctx)
      case expr: Coalesce      => ExecCoalesceContext(expr, ctx)
      case expr: Wildcard      => ExecWildcardContext(expr, ctx)
      case expr: Filter        => ExecFilterContext(expr, ctx)
      case expr: Maths         => ExecMathsContext(expr, ctx)
      case expr: If            => ExecIfContext(expr, ctx)
      case expr: Match         => ExecMatchContext(expr, ctx)
      case expr: AstArray      => ExecArrayContext(expr, ctx)
      case expr: AstObject     => ExecObjectContext(expr, ctx)
    }
  }

  def visitExpr(ctx: ExecExprContext): Unit = {
    ctx match {
      case ctx: ExecAnyContext         => visitAny(ctx)
      case ctx: ExecIndexContext       => visitIndex(ctx)
      case ctx: ExecNavContext         => visitNav(ctx)
      case ctx: ExecPathContext        => visitPath(ctx)
      case ctx: ExecSliceContext       => visitSlice(ctx)
      case ctx: ExecBlockInvokeContext => visitBlockInvoke(ctx)
      case ctx: ExecFuncContext        => visitFunc(ctx)
      case ctx: ExecCoalesceContext    => visitCoalesce(ctx)
      case ctx: ExecWildcardContext    => visitWildcard(ctx)
      case ctx: ExecFilterContext      => visitFilter(ctx)
      case ctx: ExecMathsContext       => visitMaths(ctx)
      case _                           => ()
    }
  }

  def visitFilter(ctx: ExecFilterContext): Unit = {}

  def visitAny(ctx: ExecAnyContext): Unit = {}

  def visitIndex(ctx: ExecIndexContext): Unit = {}

  def visitNav(ctx: ExecNavContext): Unit = {}

  def visitPath(ctx: ExecPathContext): Unit = {}

  def visitSlice(ctx: ExecSliceContext): Unit = {}

  def visitFunc(ctx: ExecFuncContext): Unit = {}

  def visitCoalesce(ctx: ExecCoalesceContext): Unit = {}

  def visitWildcard(ctx: ExecWildcardContext): Unit = {}

  def visitMaths(ctx: ExecMathsContext): Unit = {}

  def visitIf(ctx: ExecIfContext): Unit = {}

  def visitMatch(ctx: ExecMatchContext): Unit = {}

}
// scalastyle:on number.of.methods
