package flow

import NativeTypes._

import ast._
import llvm.Operand

// Types =======================================================================

trait Type {
  def name: String
  def toLlvm: llvm.Type
  override def toString = name
}

case class NativeType[A <: llvm.Type](name: String, toLlvm: A) extends Type

// Defs ========================================================================

case class Signature(name: String, parameterTypes: Option[Seq[Type]])

sealed trait Def {
  def name: String
  def parameterTypes: Option[Seq[Type]]
  def resultType: Type
  def signature: Signature = Signature(name, parameterTypes)
}

sealed trait FunDef extends Def {
  def requiredTypes: Seq[Type]
  def parameterTypes = Some(requiredTypes)
}

case class NativeFunDef(
  name: String,
  requiredTypes: Seq[Type],
  resultType: Type,
  body: Seq[Operand] => Operand) extends FunDef

case class TypeDef(name: String, aType: Type, defs: Map[Signature, Def])

// =============================================================================

trait DefLookup {

  def typeDefOf(aType: Type): TypeDef

  def defFor(aType: Type, name: String, argTypes: Option[Seq[Type]]) = {
    val typeDef = typeDefOf(aType)
    val sig = Signature(name, argTypes)

    typeDef.defs.get(sig) match {
      case Some(defn) =>
        defn
      case None =>
        argTypes match {
          case None =>
            error(s"Type $aType does not define $name.")
          case Some(types) =>
            error(s"Type $aType does not define $name${types.mkString("(", ",", ")")}.")
        }
    }
  }

}

trait Types {

  def unify(types: Type*): Type = {
    Unit
  }

}
