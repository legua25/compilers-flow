package flow.syntax

import scala.collection.JavaConversions._

import FlowParser._

class AstVisitor extends FlowBaseVisitor[Ast] with OperatorPrecedence {

  override def visitProgram(context: ProgramContext) =
    Program(context.statement.toList.map(visitStatement).filter(_ != null))

  override def visitStatement(context: StatementContext) =
    visit(context).asInstanceOf[Statement]

  override def visitComplexExpression(context: ComplexExpressionContext) =
    super.visitComplexExpression(context).asInstanceOf[Expression]

  override def visitExpression(context: ExpressionContext) =
    visit(context).asInstanceOf[Expression]

  override def visitMemberDefinition(context: MemberDefinitionContext) =
    visit(context).asInstanceOf[MemberDef]

  override def visitTypeDefinition(context: TypeDefinitionContext) = {
    val typeName = context.ID.getText()
    val defs = context.memberDefinition.toList.map(visitMemberDefinition)

    TypeDef(typeName, defs)
  }

  override def visitMemberDef(context: MemberDefContext) = {
    val defn = visitDefn(context.defn)

    if (context.STATIC != null)
      StaticDef(defn)
    else
      defn
  }

  override def visitMemberVarDef(context: MemberVarDefContext) = {
    val defn = visitVariableDefinition(context.variableDefinition)

    if (context.STATIC != null)
      StaticVarDef(defn)
    else
      defn
  }

  override def visitExternalMemberDef(context: ExternalMemberDefContext) = {
    val name = context.defnHead.ID.getText()
    val parameters = Option(context.defnHead.parameterClause) map {
      paramClause =>
        Option(paramClause.parameters) map {
          parameters =>
            parameters.parameter.toList.map(visitParameter)
        } getOrElse Seq()
    }
    val typeAnn = context.defnHead.typeAnn.ID.getText()

    val defn = Def(name, parameters, typeAnn, None)

    if (context.STATIC != null)
      StaticDef(defn)
    else
      defn
  }

  override def visitDefn(context: DefnContext) = {
    val name = context.defnHead.ID.getText()
    val parameters = Option(context.defnHead.parameterClause) map {
      paramClause =>
        Option(paramClause.parameters) map {
          parameters =>
            parameters.parameter.toList.map(visitParameter)
        } getOrElse Seq()
    }
    val typeAnn = context.defnHead.typeAnn.ID.getText()
    val body = visitExpression(context.expression)

    Def(name, parameters, typeAnn, Some(body))
  }

  override def visitExternalDef(context: ExternalDefContext) = {
    val name = context.defnHead.ID.getText()
    val parameters = Option(context.defnHead.parameterClause) map {
      paramClause =>
        Option(paramClause.parameters) map {
          parameters =>
            parameters.parameter.toList.map(visitParameter)
        } getOrElse Seq()
    }
    val typeAnn = context.defnHead.typeAnn.ID.getText()

    Def(name, parameters, typeAnn, None)
  }

  override def visitVariableDefinition(context: VariableDefinitionContext) = {
    val names = context.ID.toList.map(_.getText())
    val typeAnn = Option(context.typeAnn).map(_.ID.getText())
    val expr = visitExpression(context.expression)
    val isMutable = context.kw.getText() == "var"

    VarDef(names, typeAnn, expr, isMutable)
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

  override def visitFor(context: ForContext) = {
    val generators = context.generators.generator.toList.map(visitGenerator)
    val expression = visitExpression(context.expression)

    For(generators, expression)
  }

  override def visitGenerator(context: GeneratorContext) = {
    val name = context.ID.getText()
    val expression = visitExpression(context.gen)
    val guard = Option(context.guard).map(visitExpression)

    Generator(name, expression, guard)
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

  override def visitAssignment(context: AssignmentContext) = {
    val expr0 = visitExpression(context.expression(0))
    val expr1 = visitExpression(context.expression(1))

    Assignment(expr0, expr1)
  }

  override def visitBool(context: BoolContext) =
    BoolLiteral(context.BOOL.getText() == "true")

  override def visitChar(context: CharContext) = {
    val literal = context.CHAR.getText()
    val char = literal.substring(1, literal.size - 1) match {
      case "\\b"   => '\b'
      case "\\t"   => '\t'
      case "\\n"   => '\n'
      case "\\f"   => '\f'
      case "\\r"   => '\r'
      case "\\\""  => '\\'
      case "\\'"   => '\''
      case "\\\\"  => '\\'
      case literal => literal(0)
    }

    val message = "Parsed character literal " + literal + " => " + char
    flow.debug(message)
    flow.debug(literal.substring(0, literal.size - 1))

    CharLiteral(char)
  }

  override def visitString(context: StringContext) = {
    val literal = context.STRING.getText()
    StringLiteral(literal.substring(1, literal.size - 1))
  }

  override def visitDecimal(context: DecimalContext) =
    IntLiteral(BigInt(context.DECIMAL.getText()))

  override def visitHexaDecimal(context: HexaDecimalContext) =
    IntLiteral(BigInt(context.HEXADECIMAL.getText().substring(2), 16))

  override def visitOctal(context: OctalContext) =
    IntLiteral(BigInt(context.OCTAL.getText().substring(2), 8))

  override def visitBinary(context: BinaryContext) =
    IntLiteral(BigInt(context.BINARY.getText().substring(2), 2))

  override def visitFloat(context: FloatContext) =
    FloatLiteral(context.FLOAT().getText())

  override def visitParenthesized(context: ParenthesizedContext) =
    Parenthesized(visitExpression(context.expression))

  override def visitParameter(context: ParameterContext) =
    Parameter(context.ID.getText(), context.typeAnn.ID.getText())

}
