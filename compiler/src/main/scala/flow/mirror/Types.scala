package flow.mirror

import scala.collection.mutable
import flow.error
import flow.debug

sealed trait Type {

  def name: String

  def toLlvm: llvm.Type

  def companion = CompanionType(this)

  override def toString = name

}

case class SingleType[A <: llvm.Type](name: String, toLlvm: A) extends Type

case class StructureType(name: String) extends Type {

  val alias = llvm.Type.NamedType(s".$name")

  def toLlvm = alias.pointer

}

case class CompanionType(accompaniedType: Type) extends Type {

  def name = s"${accompaniedType.name}Companion"

  def toLlvm = NativeTypes.Unit.toLlvm

}

trait Types {
  self: NativeTypes =>

  val types = mutable.Map.empty[String, Type]
  val typeDefs = mutable.Map.empty[Type, TypeDef]

  def types_declare(name: String): Type = {
    val aType = NativeTypes.get(name) match {
      case Some(nativeType) => nativeType
      case None             => ???
    }

    types_declare(aType)

    aType
  }

  def types_declare(aType: Type) = {
    debug(s"Declaring: $aType.")

    if (typeDefs.contains(aType))
      error(s"Type $aType is already declared.")

    types(aType.name) = aType
    typeDefs(aType) = TypeDef(aType)

    aType
  }

  def types_define(aType: Type, defn0: Def) = {
    debug(s"Defining: $aType.${defn0.signature}: ${defn0.resultType}.")

    val defn = nativeDefFor(aType, defn0.signature) match {
      case Some(defn) => defn
      case None       => defn0
    }

    val item = defn.signature -> defn

    val newDefs = typeDefs.get(aType) match {
      case Some(TypeDef(_, defs)) =>
        if (defs.contains(defn.signature))
          error(s"$aType.${defn.signature} is already declared")

        defs + item
      case None => Map(item)
    }

    typeDefs(aType) = TypeDef(aType, newDefs)
  }

  def typeFor(name: String): Type = {
    types.get(name) match {
      case Some(aType) => aType
      case None        => error(s"Type $name is not defined.")
    }
  }

  def typeDefOf(aType: Type): TypeDef = {
    typeDefs.get(aType) match {
      case Some(typeDef) => typeDef
      case None          => error(s"Type $aType is not defined.")
    }
  }

  def defFor(aType: Type, name: String, argTypes: Option[Seq[Type]]): Option[Def] = {
    val typeDef = typeDefOf(aType)
    val signature = Signature(name, argTypes)

    typeDef.defs.get(signature)
  }

  def defFor(aType: Type, name: String): Option[Def] =
    defFor(aType, name, None)

}
