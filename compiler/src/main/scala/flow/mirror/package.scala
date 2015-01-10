package flow

package object mirror {

  //  case class CompiledExpression(value: llvm.Operand, aType: Type)
  //  implicit def tupleToCompiledExpression(tuple: (llvm.Operand, Type)) =
  //    CompiledExpression(tuple._1, tuple._2)
  type CompiledExpression = (llvm.Operand, Type)

}
