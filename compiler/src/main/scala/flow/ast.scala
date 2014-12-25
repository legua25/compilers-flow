package flow

object ast {

  sealed trait Ast

  case class Program(statements: Seq[Statement]) extends Ast {
    override def toString = statements.map(_.toString).mkString("\n")
  }

  sealed trait Statement extends Ast

  sealed trait Expression extends Statement

  case class InfixExpression(expr0: Expression, op: String, expr1: Expression) extends Expression {
    override def toString = s"($expr0 $op $expr1)"
  }

  case class Parenthesized(expression: Expression) extends Expression {
    override def toString = expression.toString
  }

  case class Call(id: String, args: Seq[Expression]) extends Expression

  case class Id(name: String) extends Expression {
    override def toString = name
  }
  //
  //  case class ExternalDef(name: String, params: Seq[Parameter], variadic: Boolean, typeAnn: String) extends Statement
  //
  //  case class InternalDef(name: String, params: Option[Seq[Parameter]], variadic: Boolean, typeAnn: Option[String], body: Expression) extends Statement
  //
  //  case class VarDef(ids: Seq[String], typeAnn: Option[String], expr: Expression, immutable: Boolean) extends Expression
  //
  //  case class Block(exprs: Seq[Expression]) extends Expression
  //
  //  case class Branch(cond: Expression, thenExpr: Expression, elseExpr: Option[Expression]) extends Expression
  //
  //  case class While(cond: Expression, body: Expression) extends Expression
  //
  //  case class Assignment(id: String, expr: Expression) extends Expression
  //
  //  case class Parameter(name: String, typeAnn: String) extends Ast

  case class BoolLiteral(value: Boolean) extends Expression

  case class CharLiteral(value: Char) extends Expression

  case class StringLiteral(value: String) extends Expression

  case class IntLiteral(value: String, base: IntegerBase) extends Expression {
    override def toString = value
  }

  case class FloatLiteral(value: String) extends Expression

  sealed trait IntegerBase
  case object Decimal extends IntegerBase
  case object HexaDecimal extends IntegerBase
  case object Octal extends IntegerBase
  case object Binary extends IntegerBase
}
