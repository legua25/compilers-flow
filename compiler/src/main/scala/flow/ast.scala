package flow

object ast {

  sealed trait Ast

  case class Program(statements: Seq[Statement]) extends Ast

  // Statement

  sealed trait Statement extends Ast

  sealed trait MemberDefinition

  case class TypeDefinition(name: String, defs: Seq[MemberDefinition]) extends Statement

  case class Definition(name: String, parameters: Option[Seq[Parameter]], typeAnn: String, body: Expression) extends Statement with MemberDefinition

  case class ExternalFunction(name: String, parameters: Seq[Parameter], typeAnn: String) extends Statement with MemberDefinition

  case class StaticDefinition(definition: Definition) extends Ast with MemberDefinition

  // Expression

  sealed trait Expression extends Statement

  sealed trait LValue

  case class Parameter(name: String, aType: String) extends Ast

  case class VarDefinition(name: String, typeAnn: String, expr: Expression, isMutable: Boolean) extends Expression with MemberDefinition

  case class If(condition: Expression, thn: Expression, els: Option[Expression]) extends Expression

  case class While(condition: Expression, body: Expression) extends Expression

  case class Block(expressions: Seq[Expression]) extends Expression

  case class PrefixExpression(op: String, expr: Expression) extends Expression

  case class InfixExpression(expr0: Expression, op: String, expr1: Expression) extends Expression

  case class Parenthesized(expression: Expression) extends Expression

  case class Id(name: String) extends Expression with LValue

  case class Selection(where: Expression, what: String) extends Expression with LValue

  case class Application(what: Expression, arguments: Seq[Expression]) extends Expression

  case class Assignment(where: LValue, what: Expression) extends Expression

  case class BoolLiteral(value: Boolean) extends Expression

  case class CharLiteral(value: Char) extends Expression

  case class StringLiteral(value: String) extends Expression

  case class IntLiteral(value: BigInt) extends Expression

  case class FloatLiteral(value: String) extends Expression

}
