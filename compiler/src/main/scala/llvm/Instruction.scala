package llvm

sealed abstract class Terminator {
  def llvm: String
}

case class Ret(returnOperand: Option[Operand]) extends Terminator {
  def llvm =
    "ret " + returnOperand.map(_.typedRepr).getOrElse("void")
}

case class CondBr(
  condition: Operand,
  trueDest: Name,
  falseDest: Name) extends Terminator {

  def llvm =
    s"br ${condition.typedRepr}, label %$trueDest, label %$falseDest"
}

case class Br(dest: Name) extends Terminator {
  def llvm =
    s"br label %$dest"
}

case class Switch(
  operand: Operand,
  defaultDest: Name,
  dests: Seq[(Constant, Name)]) extends Terminator {

  def llvm = {
    val cases = for ((const, dest) <- dests) yield s"${const.typedRepr}, label %$dest"

    s"switch ${operand.typedRepr}, label %$defaultDest [ $cases ]"
  }
}

case class IndirectBr(
  operand: Operand,
  possibleDests: Seq[Name]) extends Terminator {

  def llvm =
    ???
}

case class Invoke(
  callingConvention: CallingConvention,
  returnAttributes: Seq[ParameterAttribute],
  function: Operand,
  arguments: Seq[(Operand, Seq[ParameterAttribute])],
  functionAttributes: Seq[FunctionAttribute],
  returnDest: Name,
  exceptionDest: Name) extends Terminator {

  def llvm =
    ???
}

case class Resume(operand: Operand) extends Terminator {
  def llvm =
    s"resume ${operand.typedRepr}"
}

case object Unreachable extends Terminator {
  def llvm =
    "unreachable"
}

//sealed trait LandingPadClause
//
//object LandingPadClause {
//
//  case class Catch(constant: Constant) extends LandingPadClause
//
//  case class Filter(constant: Constant) extends LandingPadClause
//
//}

/**
 * non-terminator instructions:
 * http://llvm.org/docs/LangRef.html#binaryops>
 * http://llvm.org/docs/LangRef.html#bitwiseops>
 * http://llvm.org/docs/LangRef.html#memoryops>
 * http://llvm.org/docs/LangRef.html#otherops>
 */
sealed trait Instruction {
  def llvm: String
}

case class Add(
  operand0: Operand,
  operand1: Operand,
  nuw: Boolean = false,
  nsw: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "add",
      if (nuw) "nuw" else "",
      if (nsw) "nsw" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class FAdd(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "fadd",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class Sub(
  operand0: Operand,
  operand1: Operand,
  nuw: Boolean = false,
  nsw: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "sub",
      if (nuw) "nuw" else "",
      if (nsw) "nsw" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class FSub(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "fsub",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class Mul(
  operand0: Operand,
  operand1: Operand,
  nuw: Boolean = false,
  nsw: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "mul",
      if (nuw) "nuw" else "",
      if (nsw) "nsw" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class FMul(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "fmul",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class UDiv(
  operand0: Operand,
  operand1: Operand,
  exact: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "udiv",
      if (exact) "exact" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class SDiv(
  operand0: Operand,
  operand1: Operand,
  exact: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "sdiv",
      if (exact) "exact" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class FDiv(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "fdiv",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class URem(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "urem",
      operand0.typedRepr + ",",
      operand1.repr).mkString(" ")
  }
}

case class SRem(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "srem",
      operand0.typedRepr + ",",
      operand1.repr).mkString(" ")
  }
}

case class FRem(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "frem",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class Shl(
  operand0: Operand,
  operand1: Operand,
  nuw: Boolean = false,
  nsw: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "shl",
      if (nuw) "nuw" else "",
      if (nsw) "nsw" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class LShr(
  operand0: Operand,
  operand1: Operand,
  exact: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "lshr",
      if (exact) "exact" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class AShr(
  operand0: Operand,
  operand1: Operand,
  exact: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "ashr",
      if (exact) "exact" else "",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class And(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "and",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class Or(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "or",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class Xor(
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm = {
    Seq(
      "xor",
      operand0.typedRepr + ",",
      operand1.repr).filter(_ != "").mkString(" ")
  }
}

case class Alloca(
  aType: Type,
  nrOfElements: Option[Operand] = None,
  alignment: Int = 0) extends Instruction {

  def llvm = {
    Seq(
      "alloca",
      aType.llvm,
      nrOfElements.map(op => s", ${op.typedRepr}").getOrElse(""),
      if (alignment != 0) s", align $alignment" else "").filter(_ != "").mkString(" ")
  }
}

case class Load(
  address: Operand,
  volatile: Boolean = false,
  alignment: Int = 0) extends Instruction {

  def llvm = {
    Seq(
      "load",
      if (volatile) s"volatile" else "",
      address.typedRepr,
      if (alignment != 0) s", align $alignment" else "").filter(_ != "").mkString(" ")
  }
}

case class Store(
  value: Operand,
  address: Operand,
  volatile: Boolean = false,
  alignment: Int = 0) extends Instruction {

  def llvm = {
    Seq(
      "store",
      if (volatile) s"volatile" else "",
      value.typedRepr + ",",
      address.typedRepr,
      if (alignment != 0) s", align $alignment" else "").filter(_ != "").mkString(" ")
  }
}

case class GetElementPtr(
  address: Operand,
  indices: Seq[Operand],
  inBounds: Boolean = false) extends Instruction {

  def llvm = {
    Seq(
      "getelementptr",
      if (inBounds) s"inbounds" else "",
      address.typedRepr,
      indices.map(op => s", ${op.typedRepr}").mkString).filter(_ != "").mkString(" ")
  }
}

//case class Fence(
//  atomicity: Atomicity  ) extends Instruction
//
//case class CmpXchg(
//  volatile: Boolean,
//  address: Operand,
//  expected: Operand,
//  replacement: Operand,
//  atomicity: Atomicity  ) extends Instruction
//
//case class AtomicRMW(
//  volatile: Boolean,
//  rmwOperation: RMWOperation,
//  address: Operand,
//  value: Operand,
//  atomicity: Atomicity  ) extends Instruction
//

sealed abstract class ConvertingInstruction(name: String) extends Instruction {
  def operand: Operand
  def targetType: Type
  def llvm = s"$name ${operand.typedRepr} to ${targetType.llvm}"
}

case class Trunc(operand: Operand, targetType: Type)
  extends ConvertingInstruction("trunc")

case class ZExt(operand: Operand, targetType: Type)
  extends ConvertingInstruction("zext")

case class SExt(operand: Operand, targetType: Type)
  extends ConvertingInstruction("sext")

case class FPToUI(operand: Operand, targetType: Type)
  extends ConvertingInstruction("fptoui")

case class FPToSI(operand: Operand, targetType: Type)
  extends ConvertingInstruction("fptosi")

case class UIToFP(operand: Operand, targetType: Type)
  extends ConvertingInstruction("uitofp")

case class SIToFP(operand: Operand, targetType: Type)
  extends ConvertingInstruction("sitofp")

case class FPTrunc(operand: Operand, targetType: Type)
  extends ConvertingInstruction("fptrunc")

case class FPExt(operand: Operand, targetType: Type)
  extends ConvertingInstruction("fpext")

case class PtrToInt(operand: Operand, targetType: Type)
  extends ConvertingInstruction("ptrtoint")

case class IntToPtr(operand: Operand, targetType: Type)
  extends ConvertingInstruction("inttoptr")

case class BitCast(operand: Operand, targetType: Type)
  extends ConvertingInstruction("bitcast")

//case class AddrSpaceCast(
//  operand0: Operand,
//  aType: Type  ) extends Instruction

case class ICmp(
  predicate: IntegerPredicate,
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm =
    s"icmp ${predicate.llvm} ${operand0.typedRepr}, ${operand1.repr}"
}

case class FCmp(
  predicate: FloatingPointPredicate,
  operand0: Operand,
  operand1: Operand) extends Instruction {

  def llvm =
    s"fcmp ${predicate.llvm} ${operand0.typedRepr}, ${operand1.repr}"
}

case class Phi(
  aType: Type,
  incomingValues: Seq[(Operand, Name)]) extends Instruction {

  def llvm = {
    val values = (incomingValues map { case (op, n) => s"[ ${op.repr} %$n ]" })
      .mkString(", ")
    s"phi $aType $values"
  }
}

case class Call(
  function: Operand,
  arguments: Seq[(Operand, Seq[ParameterAttribute])],
  mustTail: Option[Boolean] = None,
  callingConvention: Option[CallingConvention] = None,
  returnAttributes: Seq[ParameterAttribute] = Seq(),
  functionAttributes: Seq[FunctionAttribute] = Seq()) extends Instruction {

  def llvm = {
    val args = (arguments map {
      case (arg, Seq()) =>
        arg.typedRepr
      case (arg, attrs) =>
        s"${arg.typedRepr} ${attrs.map(_.llvm).mkString(" ")}"
    }).mkString("(", ", ", ")")

    Seq(
      mustTail.map(mt => if (mt) "musttail" else "tail").getOrElse(""),
      "call",
      callingConvention.map(_.llvm).getOrElse(""),
      returnAttributes.map(_.llvm).mkString(" "),
      function.typedRepr + args,
      functionAttributes.map(_.llvm).mkString(" ")).filter(_ != "").mkString(" ")
  }
}

//case class Select(
//  condition: Operand,
//  trueValue: Operand,
//  falseValue: Operand  ) extends Instruction
//
//case class VAArg(
//  argList: Operand,
//  aType: Type  ) extends Instruction
//
//case class ExtractElement(
//  vector: Operand,
//  index: Operand  ) extends Instruction
//
//case class InsertElement(
//  vector: Operand,
//  element: Operand,
//  index: Operand  ) extends Instruction
//
//case class ShuffleVector(
//  operand0: Operand,
//  operand1: Operand,
//  mask: Constant  ) extends Instruction
//
//case class ExtractValue(
//  aggregate: Operand,
//  indices: Seq[Int]  ) extends Instruction
//
//case class InsertValue(
//  aggregate: Operand,
//  element: Operand,
//  indices: Seq[Int]  ) extends Instruction
//
//case class LandingPad(
//  aType: Type,
//  personalityFunction: Operand,
//  cleanup: Boolean,
//  clauses: Seq[LandingPadClause]  ) extends Instruction
