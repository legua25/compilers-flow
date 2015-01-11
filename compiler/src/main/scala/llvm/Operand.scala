package llvm

import java.lang.{ String => SString }

sealed trait Operand {
  def aType: Type
  def repr: SString
  def typedRepr: SString
}

abstract class TypedOperand(val aType: Type, val repr: SString) extends Operand {
  def typedRepr = s"${aType.llvm} $repr"
}

/**
 * http://llvm.org/docs/LangRef.html#constants
 */
sealed trait Constant extends Operand

abstract class TypedConstant(val aType: Type, val repr: SString) extends Constant {
  def typedRepr = s"${aType.llvm} $repr"
}

abstract class UntypedConstant(val repr: SString) extends Constant {
  def aType = Type.Void
  def typedRepr = repr
}

object Constant {

  case object True extends TypedConstant(Type.Int(1), "true")

  case object False extends TypedConstant(Type.Int(1), "false")

  case class Int(override val aType: Type.Int, value: SString)
    extends TypedConstant(aType, value)

  case class Float(override val aType: FloatingPointType, value: SString)
    extends TypedConstant(aType, value)

  case class Null[A <: Type](override val aType: Type.Pointer[A])
    extends TypedConstant(aType, "null")

  private def makeStructRepr(elements: Seq[Constant], isPacked: Boolean): SString = {
    val (open, close) = if (isPacked) ("<{", "}>") else ("{", "}")
    elements.map(_.typedRepr).mkString(open, ", ", close)
  }

  case class Struct(elements: Seq[Constant], isPacked: Boolean)
    extends UntypedConstant(makeStructRepr(elements, isPacked))

  case class Array(elementType: Type, elements: Seq[Constant])
    extends UntypedConstant(elements.map(_.typedRepr).mkString("[", ", ", "]"))

  case class String(value: SString)
    extends UntypedConstant("c\"" + value + "\\00\"")

  case class Vector(elements: Seq[Constant])
    extends UntypedConstant(elements.map(_.typedRepr).mkString("<", ", ", ">"))

  case object ZeroInitializer extends UntypedConstant("zeroinitializer")

  case object Undef extends UntypedConstant("undef")

  //  case class BlockAddress(functionName: Name, blockName: Name)
  //    extends Constant]

}

case class GlobalReference(override val aType: Type, name: Name) extends TypedOperand(aType, s"@$name")

case class LocalReference(override val aType: Type, name: Name) extends TypedOperand(aType, s"%$name")
