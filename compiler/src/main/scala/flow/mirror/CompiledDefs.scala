package flow.mirror

import flow.GlobalCodegen
import flow.BlockCodegen

trait CompiledDefs {
  self: GlobalCodegen with BlockCodegen =>

  // TODO: this is not actually used
  trait Ref {

    def reference: llvm.Operand

  }

  object Ref {

    def unapply(ref: Ref): Option[llvm.Operand] =
      Some(ref.reference)

  }

  case class ConstantDef(
    name: String,
    aType: Type,
    constant: llvm.Constant) extends VarDef {

    def isMutable = false

    def compile(arguments: Seq[llvm.Operand]) =
      constant
  }

  case class LocalVarDef(
    name: String,
    aType: Type,
    isMutable: Boolean,
    reference: llvm.LocalReference) extends VarDef with Ref {

    def compile(arguments: Seq[llvm.Operand]) =
      load(aType.toLlvm, reference)

  }

  case class GlobalDef(
    name: String,
    parameterTypes: Option[Seq[Type]],
    resultType: Type,
    reference: llvm.GlobalReference) extends Def with Ref {

    def compile(arguments: Seq[llvm.Operand]) = {
      call(resultType.toLlvm, reference, arguments)
    }

  }

  case class StructureReturningDef(
    name: String,
    parameterTypes: Option[Seq[Type]],
    resultType: StructureType,
    reference: llvm.GlobalReference) extends Def with Ref {

    def compile(arguments: Seq[llvm.Operand]) = {
      val struct = alloca(resultType.alias)
      call(reference, struct +: arguments)
      struct
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
