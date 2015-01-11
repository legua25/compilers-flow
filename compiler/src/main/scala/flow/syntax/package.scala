package flow

package object syntax {

  sealed trait Ast extends Positioned

  case class Program(statements: Seq[Statement]) extends Ast

  // Statement

  sealed trait Statement extends Ast

  sealed trait MemberDef extends Ast

  case class TypeDef(name: String, defs: Seq[MemberDef]) extends Statement

  case class Def(name: String, parameters: Option[Seq[Parameter]], typeAnn: String, body: Option[Expression]) extends Statement with MemberDef

  case class StaticDef(definition: Def) extends Ast with MemberDef

  case class StaticVarDef(definition: VarDef) extends Ast with MemberDef

  // Expression

  sealed trait Expression extends Statement

  case class Parameter(name: String, typeAnn: String) extends Ast

  case class VarDef(name: String, typeAnn: Option[String], expr: Expression, isMutable: Boolean) extends Expression with MemberDef

  case class If(condition: Expression, thn: Expression, els: Option[Expression]) extends Expression

  case class While(condition: Expression, body: Expression) extends Expression

  case class Block(expressions: Seq[Expression]) extends Expression

  //  case class PrefixExpression(operator: String, expression: Expression) extends Expression

  case class InfixExpression(expression0: Expression, operator: String, expression1: Expression) extends Expression

  case class Parenthesized(expression: Expression) extends Expression

  case class Id(name: String) extends Expression

  case class Selection(expression: Expression, name: String) extends Expression

  case class Application(expression: Expression, arguments: Seq[Expression]) extends Expression

  case class Assignment(where: Expression, what: Expression) extends Expression

  case class BoolLiteral(value: Boolean) extends Expression

  case class CharLiteral(value: Char) extends Expression

  case class StringLiteral(value: String) extends Expression

  case class IntLiteral(value: BigInt) extends Expression

  case class FloatLiteral(value: String) extends Expression

}
