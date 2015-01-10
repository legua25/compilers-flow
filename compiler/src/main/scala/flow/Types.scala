package flow

import scala.collection.mutable

sealed trait Type {

  def name: String

  def toLlvm: llvm.Type

  def companion = CompanionType(this)

  override def toString = name

}

case class NativeType[A <: llvm.Type](name: String, toLlvm: A) extends Type

case class DefinedType(name: String) extends Type {

  def toLlvm = { println(name); llvm.Type.Void }

}

case class CompanionType(accompaniedType: Type) extends Type {

  def name = s"${accompaniedType.name}Companion"

  def toLlvm = NativeTypes.Unit.toLlvm

}

trait Types {
  self: NativeTypes =>

  val typeDefs = mutable.Map.empty[String, TypeDef]

  def declare(name: String): Type = {
    val aType = NativeTypes.get(name) match {
      case Some(nativeType) => nativeType
      case None             => DefinedType(name)
    }

    declare(aType)

    aType
  }

  def declare(aType: Type): Type = {
    println(s"declaring: $aType")
    val typeName = aType.name

    if (typeDefs.contains(typeName))
      error(s"Type $typeName is already declared.")

    typeDefs(typeName) = TypeDef(aType)

    aType
  }

  // TODO: this sucks
  def declare(aType: Type, defn: Def) = {
    println(s"declaring: $aType.${defn.signature}")
    val item = defn.signature -> defn

    val newDefs = typeDefs.get(aType.name) match {
      case Some(TypeDef(_, defs)) => defs + item
      case None                   => Map(item)
    }

    typeDefs(aType.name) = TypeDef(aType, newDefs)
  }

  def define(typeDef: TypeDef) = {
    val name = typeDef.name
    val aType = typeDef.aType
    println(s"defining: $aType")

    if (!typeDefs.contains(name))
      error(s"Type $name is not declared.")

    //    if (typeDefs.contains(aType))
    //      error(s"Type $name is already defined.")

    typeDefs(aType.name) = typeDef
  }

  def typeFor(name: String): Type = {
    typeDefs.get(name) match {
      case Some(typeDef) => typeDef.aType
      case None          => error(s"Type $name is not defined.")
    }
  }

  def typeDefOf(aType: Type): TypeDef = {
    typeDefs.get(aType.name) match {
      case Some(typeDef) => typeDef
      case None          => error(s"Type $aType is not defined.")
    }
  }

  def defFor(aType: Type, name: String, argTypes: Option[Seq[Type]]) = {
    val typeDef = typeDefOf(aType)
    val signature = Signature(name, argTypes)

    typeDef.defs.get(signature) match {
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
