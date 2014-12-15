package flow

object ast {

  sealed trait Ast

  case class Program(statements: Seq[Statement]) extends Ast

  trait Statement extends Ast

  trait Expression extends Statement

  case class ExternalDef(name: String, params: Seq[Parameter], variadic: Boolean, typeAnn: String) extends Statement

  case class InternalDef(name: String, params: Option[Seq[Parameter]], variadic: Boolean, typeAnn: Option[String], body: Expression) extends Statement

  case class VarDef(ids: Seq[String], typeAnn: Option[String], expr: Expression, immutable: Boolean) extends Expression

  case class Block(exprs: Seq[Expression]) extends Expression

  case class Branch(cond: Expression, thenExpr: Expression, elseExpr: Option[Expression]) extends Expression

  case class While(cond: Expression, body: Expression) extends Expression

  case class Assignment(id: String, expr: Expression) extends Expression

  case class Call(id: String, args: Seq[Expression]) extends Expression

  case class Id(name: String) extends Expression

  case class Parameter(name: String, typeAnn: String) extends Ast

  case class BoolLiteral(value: String) extends Expression

  case class CharLiteral(value: String) extends Expression

  case class StringLiteral(value: String) extends Expression

  case class IntLiteral(value: String) extends Expression

  case class FloatLiteral(value: String) extends Expression

}
