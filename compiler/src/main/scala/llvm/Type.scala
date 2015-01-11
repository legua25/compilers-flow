package llvm

/**
 * <http://llvm.org/docs/LangRef.html#type-system>
 */
sealed abstract class Type(val llvm: String) {
  def pointer = Type.Pointer(this, None)
}

/**
 * http://llvm.org/docs/LangRef.html#first-class-types
 */
abstract class FirstClassType(llvm: String) extends Type(llvm)

/**
 * http://llvm.org/docs/LangRef.html#single-value-types
 */
abstract class SingleValueType(llvm: String) extends FirstClassType(llvm)

/**
 * <http://llvm.org/docs/LangRef.html#floating-point-types>
 */
abstract class FloatingPointType(llvm: String) extends SingleValueType(llvm)

/**
 * http://llvm.org/docs/LangRef.html#aggregate-types
 */
abstract class AggregateType(llvm: String) extends FirstClassType(llvm)

object Type {
  /**
   * <http://llvm.org/docs/LangRef.html#void-type>
   */
  case object Void extends Type("void") {
    override def pointer = Int(8).pointer
  }

  private def makeFunctionType(returnType: Type, parameterTypes: Seq[Type], isVariadic: Boolean): String = {
    val parameters = parameterTypes.map(_.llvm).mkString(", ")
    val parameterClause = "(" + (if (isVariadic) parameters + ", ..." else parameters) + ")"
    s"$returnType $parameterClause"
  }

  /**
   * <http://llvm.org/docs/LangRef.html#function-type>
   */
  case class FunctionType(returnType: Type, parameterTypes: Seq[Type], isVariadic: Boolean)
    extends Type(makeFunctionType(returnType, parameterTypes, isVariadic))

  /**
   * <http://llvm.org/docs/LangRef.html#integer-type>
   */
  case class Int(bitWidth: scala.Int) extends SingleValueType(s"i$bitWidth")

  case object Half extends FloatingPointType("half")

  case object Float extends FloatingPointType("float")

  case object Double extends FloatingPointType("double")

  case object FP128 extends FloatingPointType("fp128")

  case object X86_FP80 extends FloatingPointType("x86_fp80")

  case object PPC_FP128 extends FloatingPointType("ppc_fp128")

  /**
   * http://llvm.org/docs/LangRef.html#x86-mmx-type
   */
  case object X86_MMX extends SingleValueType("x86_mmx")

  private def makePointerType(refType: Type, addrSpace: Option[AddrSpace]): String = {
    if (addrSpace.isEmpty)
      s"${refType.llvm}*"
    else
      s"${refType.llvm} ${addrSpace.get.llvm}*"
  }

  /**
   * <http://llvm.org/docs/LangRef.html#pointer-type>
   */
  case class Pointer[A <: Type](refType: A, addrSpace: Option[AddrSpace])
    extends SingleValueType(makePointerType(refType, addrSpace))

  /**
   * <http://llvm.org/docs/LangRef.html#vector-type>
   */
  case class Vector[A <: Type](nrOfElements: scala.Int, elementType: A)
    extends SingleValueType(s"<$nrOfElements x ${elementType.llvm}>")

  case object Label extends FirstClassType("label") {
    override def pointer = Int(8).pointer
  }

  /**
   * http://llvm.org/docs/LangRef.html#metadata-type
   */
  case object Metadata extends FirstClassType("metadata")

  /**
   * <http://llvm.org/docs/LangRef.html#array-type>
   */
  case class Array[A <: Type](nrOfElements: scala.Int, elementType: A)
    extends AggregateType(s"[$nrOfElements x ${elementType.llvm}]")

  private def makeStructureType(elementTypes: Seq[Type], isPacked: Boolean): String = {
    val types = elementTypes.map(_.llvm).mkString(", ")
    if (isPacked)
      "<{ " + types + " }>"
    else
      "{ " + types + " }"
  }

  /**
   * <http://llvm.org/docs/LangRef.html#structure-type>
   */
  case class Structure(elementTypes: Seq[Type], isPacked: Boolean)
    extends AggregateType(makeStructureType(elementTypes, isPacked))

  /**
   * <http://llvm.org/docs/LangRef.html#opaque-structure-types>
   */
  case class NamedType(name: Name) extends AggregateType(s"%$name")

}
