package flow.mirror

import flow.GlobalCodegen
import flow.BlockCodegen

trait CompiledDefs {
  self: GlobalCodegen with BlockCodegen =>

  case class LocalVarDef(
    name: String,
    aType: Type,
    isMutable: Boolean,
    reference: llvm.LocalReference) extends VarDef {

    def compile(arguments: Seq[llvm.Operand]) =
      load(aType.toLlvm, reference)

  }

  case class GlobalDef(
    name: String,
    parameterTypes: Option[Seq[Type]],
    resultType: Type,
    reference: llvm.GlobalReference) extends Def {

    def compile(arguments: Seq[llvm.Operand]) = {
      call(resultType.toLlvm, reference, arguments)
    }

  }

  case class NativeFunDef(
    name: String,
    parameterTypes: Option[Seq[Type]],
    resultType: Type,
    body: Seq[llvm.Operand] => llvm.Operand) extends Def {

    def compile(arguments: Seq[llvm.Operand]) =
      body(arguments)

  }

}
