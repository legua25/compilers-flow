package flow

import scala.collection.JavaConversions._
import FlowParser._
import ast._

class AstVisitor extends FlowBaseVisitor[Ast] with OperatorPrecedence {
  override def visitProgram(context: ProgramContext) =
    Program(context.statement.toList.map(visitStatement).filter(_ != null))

  override def visitStatement(context: StatementContext) =
    visit(context).asInstanceOf[Statement]

  override def visitComplexExpression(context: ComplexExpressionContext) =
    super.visitComplexExpression(context).asInstanceOf[Expression]

  override def visitExpression(context: ExpressionContext) =
    visit(context).asInstanceOf[Expression]

  override def visitDefn(context: DefnContext) = {
    val name = context.ID.getText()
    val parameters = Option(context.parameterClause) map {
      paramClause =>
        Option(paramClause.parameters) map {
          parameters =>
            parameters.parameter.toList.map(visitParameter)
        } getOrElse Seq()
    }
    val typeAnn = context.typeAnn.ID.getText()
    val body = visitExpression(context.expression)

    Definition(name, parameters, typeAnn, body)
  }

  override def visitExternalFun(context: ExternalFunContext) = {
    val name = context.ID.getText()
    val parameters = Option(context.parameterClause.parameters) map {
      parameters =>
        parameters.parameter.toList.map(visitParameter)
    } getOrElse Seq()
    val typeAnn = context.typeAnn.ID.getText()

    ExternalFunction(name, parameters, typeAnn)
  }

  override def visitStaticDef(context: StaticDefContext) =
    StaticDefinition(visitDefn(context.defn))

  override def visitVariableDefinition(context: VariableDefinitionContext) = {
    val name = context.ID.getText()
    val typeAnn = context.typeAnn.ID.getText()
    val expr = visitExpression(context.expression)
    val isMutable = context.kw.getText() == "var"

    VarDefinition(name, typeAnn, expr, isMutable)
  }

  override def visitIf(context: IfContext) = {
    val condition = visitExpression(context.cond)
    val thn = visitExpression(context.thn)
    val els = Option(context.els).map(visitExpression)

    If(condition, thn, els)
  }

  override def visitWhile(context: WhileContext) = {
    val condition = visitExpression(context.cond)
    val body = visitExpression(context.body)

    While(condition, body)
  }

  override def visitBlock(context: BlockContext) =
    Block(context.complexExpression.toList.map(visitComplexExpression).filter(_ != null))

  //  override def visitPrefixExpression(context: PrefixExpressionContext) = {
  //    val op = context.ID.getText()
  //    val expr = visitExpression(context.expression)
  //
  //    PrefixExpression(op, expr)
  //  }

  override def visitInfixExpression(context: InfixExpressionContext) = {
    val expr0 = visitExpression(context.expression(0))
    val expr1 = visitExpression(context.expression(1))
    val op = context.ID.getText()

    reparented(InfixExpression(expr0, op, expr1))
  }

  override def visitApplication(context: ApplicationContext) = {
    val expr = visitExpression(context.expression)
    val arguments = Option(context.arguments)
      .map(_.expression.toList.map(visitExpression))
      .getOrElse(Seq())

    Application(expr, arguments)
  }

  override def visitId(context: IdContext) =
    Id(context.ID.getText())

  override def visitSelection(context: SelectionContext) = {
    val where = visitExpression(context.expression)
    val what = context.ID.getText

    Selection(where, what)
  }

  override def visitIdAssignment(context: IdAssignmentContext) = {
    val id = Id(context.ID.getText())
    val expr = visitExpression(context.expression)

    Assignment(id, expr)
  }

  override def visitSelectionAssignment(context: SelectionAssignmentContext) = {
    val where = visitExpression(context.expression(0))
    val what = context.ID.getText
    val expr = visitExpression(context.expression(1))

    Assignment(Selection(where, what), expr)
  }

  override def visitBool(context: BoolContext) =
    BoolLiteral(context.BOOL.getText() == "true")

  override def visitChar(context: CharContext) =
    CharLiteral(context.CHAR.getText().head)

  override def visitString(context: StringContext) =
    StringLiteral(context.STRING.getText())

  override def visitDecimal(context: DecimalContext) =
    IntLiteral(BigInt(context.DECIMAL.getText()))

  override def visitHexaDecimal(context: HexaDecimalContext) =
    IntLiteral(BigInt(context.HEXADECIMAL.getText().substring(2), 16))

  override def visitOctal(context: OctalContext) =
    IntLiteral(BigInt(context.OCTAL.getText().substring(1), 8))

  override def visitBinary(context: BinaryContext) =
    IntLiteral(BigInt(context.BINARY.getText().substring(2), 2))

  override def visitFloat(context: FloatContext) =
    FloatLiteral(context.FLOAT().getText())

  override def visitParenthesized(context: ParenthesizedContext) =
    Parenthesized(visitExpression(context.expression))

  override def visitParameter(context: ParameterContext) =
    Parameter(context.ID.getText(), context.typeAnn.ID.getText())

}
