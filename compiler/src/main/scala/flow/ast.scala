package flow

object ast {

  sealed trait Ast

  case class Program(statements: Seq[Statement]) extends Ast

  sealed trait Statement extends Ast

  case class FunDef(name: String, parameters: Option[Seq[Parameter]], typeAnn: String, body: Expression) extends Statement

  sealed trait Expression extends Statement

  case class VarDef(name: String, typeAnn: String, expr: Expression, isMutable: Boolean) extends Expression

  case class If(condition: Expression, thn: Expression, els: Option[Expression]) extends Expression

  case class While(condition: Expression, body: Expression) extends Expression

  case class Block(expressions: Seq[Expression]) extends Expression

  case class InfixExpression(expr0: Expression, op: String, expr1: Expression) extends Expression

  case class Parenthesized(expression: Expression) extends Expression

  sealed trait LValue extends Expression

  case class Id(name: String) extends LValue

  case class Selection(where: Expression, what: String) extends LValue

  case class Application(what: Expression, arguments: Seq[Expression]) extends Expression

  case class Assignment(where: LValue, what: Expression) extends Expression

  case class BoolLiteral(value: Boolean) extends Expression

  case class CharLiteral(value: Char) extends Expression

  case class StringLiteral(value: String) extends Expression

  case class IntLiteral(value: BigInt) extends Expression

  case class FloatLiteral(value: String) extends Expression

  case class Parameter(name: String, aType: String) extends Ast

}
