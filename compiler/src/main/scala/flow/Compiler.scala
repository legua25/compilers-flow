package flow

import ast._
import llvm.{ Call => _, _ }

trait Compiler extends GlobalCodegen with NativeTypes with OperatorPrecedence {

  val printInt =
    define(
      Function(
        returnType = Type.Void,
        name = "printInt",
        parameters = Seq(Parameter(Type.Int(64), "i"))))

  def compile(moduleName: String, program: Program): Module = {
    setBlock(newBlock("entry"))

    program.statements.foreach(compile)

    terminator(Ret(Some(Constant.Int(Type.Int(32), "0"))))

    define(
      Function(
        returnType = llvm.Type.Int(32),
        name = "main",
        basicBlocks = makeBasicBlocks()))

    module(moduleName)
  }

  def compile(statement: Statement): Unit = statement match {
    case e: Expression =>
      instruction(llvm.Call(printInt, Seq((compile(e), Seq()))))
  }

  def compile(expression: Expression): Operand = expression match {
    case InfixExpression(e0, op, e1) =>
      if (associativityOf(op) == Left) {
        compile(e0)
      }
    case _: Call              => ???
    case _: Id                => ???
    case BoolLiteral(value)   => ???
    case CharLiteral(value)   => ???
    case StringLiteral(value) => ???
    case IntLiteral(value, base) =>
      instruction(int, Add(Constant.Int(int, "0"), Constant.Int(int, value)))
    case FloatLiteral(value) => ???
    case Parenthesized(e)    => compile(e)
  }
}
