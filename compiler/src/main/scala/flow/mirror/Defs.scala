package flow.mirror

import flow.error

case class TypeDef(aType: Type, defs: Map[Signature, Def]) {

  def name = aType.name

}

object TypeDef {

  def apply(aType: Type, defs: Def*): TypeDef =
    TypeDef(aType, defs.map(d => d.signature -> d).toMap)

}

case class Parameter(name: String, aType: Type)

object This {

  def apply(aType: Type): Parameter =
    Parameter("this", aType)

}

case class Signature(name: String, parameterTypes: Option[Seq[Type]]) {

  override def toString = {
    val paramClause =
      parameterTypes
        .map(_.mkString("(", ",", ")"))
        .getOrElse("")

    s"$name$paramClause"
  }

}

trait Def {

  def name: String

  def parameterTypes: Option[Seq[Type]]

  def resultType: Type

  def signature = Signature(name, parameterTypes)

  // TODO: should this be here?
  def compile(arguments: Seq[llvm.Operand] = Seq()): llvm.Operand

}

object Def {

  def unapply(defn: Def): Option[(String, Option[Seq[Type]], Type)] =
    Some((defn.name, defn.parameterTypes, defn.resultType))

}

trait VarDef extends Def {

  def aType: Type

  def isMutable: Boolean

  def parameterTypes = None

  def resultType = aType

}

object VarDef {

  def unapply(varDef: VarDef): Option[(String, Type, Boolean)] =
    Some((varDef.name, varDef.aType, varDef.isMutable))

}
