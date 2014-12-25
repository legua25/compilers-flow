package flow

import llvm._

case class NativeDef(returnType: Type, body: Seq[Operand] => Operand)

trait NativeTypes { self: BlockCodegen =>
  val Bool = llvm.Type.Int(1)
  val Char = llvm.Type.Int(8)
  val Int = llvm.Type.Int(64)
  val Float = llvm.Type.Double

  val nativeTypes = Map(
    "Int" -> Map(
      "+" -> NativeDef(Int, {
        case Seq(a, b) => instruction(Int, Add(a, b))
      })))
}
