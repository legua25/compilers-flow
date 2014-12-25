package flow

import scala.collection.JavaConversions._
import FlowParser._
import ast._

class AstVisitor extends FlowBaseVisitor[Ast] with OperatorPrecedence {
  override def visitProgram(context: ProgramContext) = {
    Program(context.statement.toList.map(visitStatement).filter(_ != null))
  }

  override def visitStatement(context: StatementContext) =
    super.visitStatement(context).asInstanceOf[Statement]

  override def visitExpression(context: ExpressionContext) =
    visit(context).asInstanceOf[Expression]

  override def visitInfixExpr(context: InfixExprContext) = {
    val expr0 = visitExpression(context.expression(0))
    val expr1 = visitExpression(context.expression(1))
    val op = context.ID.getText()

    reparented(InfixExpression(expr0, op, expr1))
  }

  override def visitCall(context: CallContext) = {
    val id = context.ID.getText()
    val arguments = context.arguments.expression.map(visitExpression)

    Call(id, arguments)
  }

  override def visitId(context: IdContext) =
    Id(context.ID.getText())

  override def visitBool(context: BoolContext) =
    BoolLiteral(context.BOOL.getText() == "true")

  override def visitChar(context: CharContext) =
    CharLiteral(context.CHAR.getText().head)

  override def visitString(context: StringContext) =
    StringLiteral(context.STRING.getText())

  override def visitDecimal(context: DecimalContext) =
    IntLiteral(context.DECIMAL.getText(), Decimal)

  override def visitHexaDecimal(context: HexaDecimalContext) =
    IntLiteral(context.HEXADECIMAL.getText().substring(2), HexaDecimal)

  override def visitOctal(context: OctalContext) =
    IntLiteral(context.OCTAL.getText(), Octal)

  override def visitBinary(context: BinaryContext) =
    IntLiteral(context.BINARY.getText().substring(2), Binary)

  override def visitFloat(context: FloatContext) =
    FloatLiteral(context.FLOAT().getText())

  override def visitParenthesized(context: ParenthesizedContext) =
    Parenthesized(visitExpression(context.expression))

  //  override def visitVarVarDef(context: VarVarDefContext) = {
  //    val defRest = context.defRest
  //    val declIds = defRest.defPat.declIds
  //    val ids = declIds.Id.map(_.getText())
  //    val typeAnn = Option(declIds.typeAnn).map(_.Id.getText())
  //    val expr = visit(defRest.expr).asInstanceOf[Expression]
  //
  //    VarDef(ids, typeAnn, expr, false)
  //  }
  //
  //  override def visitValVarDef(context: ValVarDefContext) = {
  //    val defRest = context.defRest
  //    val declIds = defRest.defPat.declIds
  //    val ids = declIds.Id.map(_.getText())
  //    val typeAnn = Option(declIds.typeAnn).map(_.Id.getText())
  //    val expr = visit(defRest.expr).asInstanceOf[Expression]
  //
  //    VarDef(ids, typeAnn, expr, true)
  //  }
  //
  //  override def visitBlock(context: BlockContext) =
  //    Block(context.expr.map(s => visit(s).asInstanceOf[Expression]))
  //
  //  override def visitBranch(context: BranchContext) = {
  //    val cond = visit(context.cond).asInstanceOf[Expression]
  //    val thenExpr = visit(context.thenE).asInstanceOf[Expression]
  //    val elseExpr = Option(context.elseE).map(e => visit(e).asInstanceOf[Expression])
  //
  //    Branch(cond, thenExpr, elseExpr)
  //  }
  //
  //  override def visitWhile(context: WhileContext) = {
  //    val cond = visit(context.cond).asInstanceOf[Expression]
  //    val body = visit(context.body).asInstanceOf[Expression]
  //
  //    While(cond, body)
  //  }
  //
  //  override def visitAssignment(context: AssignmentContext) = {
  //    val id = context.Id.getText()
  //    val expr = visit(context.expr).asInstanceOf[Expression]
  //
  //    Assignment(id, expr)
  //  }
  //
  //  override def visitCall(context: CallContext) = {
  //    val id = context.Id.getText()
  //    val args =
  //      if (context.args == null) Seq.empty[Expression]
  //      else context.args.expr.map(e => visit(e).asInstanceOf[Expression])
  //
  //    Call(id, args)
  //  }
  //
  //  override def visitId(context: IdContext) =
  //    Id(context.Id.getText())
  //
  //  override def visitExternalDef(context: ExternalDefContext) = {
  //    val name = context.defSig.Id.getText()
  //    val paramClause = context.defSig.paramClause
  //    val params = paramClause.params.param.map(p => visit(p).asInstanceOf[Parameter])
  //    val typeAnn = context.typeAnn.Id.getText
  //
  //    ExternalDef(name, params, false, typeAnn)
  //  }
  //
  //  override def visitInternalDef(context: InternalDefContext) = {
  //    val name = context.defSig.Id.getText()
  //    val paramClause = Option(context.defSig.paramClause)
  //    val params = paramClause map { clause =>
  //      if (clause.params == null) Seq.empty[Parameter]
  //      else clause.params.param.map(p => visit(p).asInstanceOf[Parameter])
  //    }
  //    val typeAnn = Option(context.typeAnn).map(t => t.Id.getText)
  //    val body = visit(context.expr).asInstanceOf[Expression]
  //
  //    InternalDef(name, params, false, typeAnn, body)
  //  }
  //
  //  override def visitParam(context: ParamContext) =
  //    Parameter(context.Id.getText, context.typeAnn.Id.getText)
}
