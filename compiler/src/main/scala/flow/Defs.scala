package flow

case class TypeDef(aType: Type, defs: Map[Signature, Def]) {

  def name = aType.name

}

object TypeDef {

  def apply(aType: Type, defs: Def*): TypeDef =
    TypeDef(aType, defs.map(d => d.signature -> d).toMap)

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

sealed trait Def {

  def name: String

  def parameterTypes: Option[Seq[Type]]

  def resultType: Type

  def signature = Signature(name, parameterTypes)

}

object Def {

  def unapply(defn: Def): Option[(String, Option[Seq[Type]], Type)] =
    Some((defn.name, defn.parameterTypes, defn.resultType))

}

object RefDef {

  def unapply(defn: Def): Option[(String, Option[Seq[Type]], Type, llvm.Operand)] = {
    defn match {
      case GlobalDef(name, parameterTypes, resultType, reference) =>
        Some((name, parameterTypes, resultType, reference))
      case vd @ GlobalVarDef(name, _, _, reference) =>
        Some((name, None, vd.aType, reference))
      case vd @ LocalVarDef(name, _, _, reference) =>
        Some((name, None, vd.aType, reference))
      case t @ This(aType) =>
        Some((t.name, None, aType, t.reference))
      case _ =>
        None
    }
  }

}

sealed trait VarDef extends Def {

  def aType: Type

  def isMutable: Boolean

  def parameterTypes = None

  def resultType = aType

}

object VarDef {

  def unapply(varDef: VarDef): Option[(String, Type, Boolean, llvm.Operand)] = {
    varDef match {
      case GlobalVarDef(name, aType, isMutable, reference) =>
        Some((name, aType, isMutable, reference))
      case LocalVarDef(name, aType, isMutable, reference) =>
        Some((name, aType, isMutable, reference))
      case t @ This(aType) =>
        Some(t.name, aType, t.isMutable, t.reference)
      case ConstantDef(name, aType, constant) =>
        Some((name, aType, false, constant))
    }
  }

}

case class This(aType: Type) extends VarDef {

  def name = "this"

  def isMutable = false

  def reference = llvm.LocalReference(aType.toLlvm, name)

}

case class GlobalDef(
  name: String,
  parameterTypes: Option[Seq[Type]],
  resultType: Type,
  reference: llvm.GlobalReference) extends Def

case class GlobalVarDef(
  name: String,
  aType: Type,
  isMutable: Boolean,
  reference: llvm.GlobalReference) extends VarDef

case class LocalVarDef(
  name: String,
  aType: Type,
  isMutable: Boolean,
  reference: llvm.LocalReference) extends VarDef

case class ConstantDef(
  name: String,
  aType: Type,
  constant: llvm.Operand) extends VarDef {

  def isMutable = false
}

sealed trait FunDef extends Def {

  def requiredTypes: Seq[Type]

  def parameterTypes = Some(requiredTypes)

}

trait NativeDefs {
  self: BlockCodegen =>

  case class NativeFunDef(
    name: String,
    requiredTypes: Seq[Type],
    resultType: Type,
    body: Seq[llvm.Operand] => llvm.Operand) extends FunDef

}
