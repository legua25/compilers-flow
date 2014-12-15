package flow

import scala.collection.mutable

import llvm._

class BlockState(name: String) {
  val instructions = mutable.ListBuffer.empty[(Option[String], Instruction)]
  var terminator = Option.empty[Terminator]

  def basicBlock = {
    if (terminator.isEmpty)
      error(s"Unterminated BasicBlock: $name")

    BasicBlock(name, instructions.toList, terminator.get)
  }
}

trait BlockCodegen {
  val blockFor = mutable.Map.empty[String, BlockState]
  val blocks = mutable.ListBuffer.empty[BlockState]

  var currentBlock: BlockState = _

  var unnamedCount = 0

  def newUnnamed() = {
    val n = unnamedCount
    unnamedCount += 1
    s".$n"
  }

  def uniqueBlockName(name0: String) = {
    if (!blockFor.isDefinedAt(name0)) {
      name0
    }
    else {
      var i = 0
      var name = name0 + i
      while (!blockFor.isDefinedAt(name)) {
        i += 1
        name = name0 + i
      }
      name
    }
  }

  def newBlock(name0: String) = {
    val name = uniqueBlockName(name0)
    val block = new BlockState(name)
    blockFor(name) = block
    blocks += block
    name
  }

  def setBlock(name: String) = {
    if (!blockFor.isDefinedAt(name))
      error(s"Undefined block: $name")

    currentBlock = blockFor(name)
  }

  def basicBlocks = {
    val result = blocks.map(_.basicBlock).toList

    blockFor.clear()
    blocks.clear()
    currentBlock = null
    unnamedCount = 0

    result
  }

  def instruction(instr: Instruction): Unit = {
    if (currentBlock.terminator.isEmpty)
      currentBlock.instructions += ((None, instr))
  }

  def instruction(aType: FirstClassType, instr: Instruction) = {
    val n = newUnnamed()

    if (currentBlock.terminator.isEmpty)
      currentBlock.instructions += ((Some(n), instr))

    LocalReference(aType, n)
  }

  def terminator(term: Terminator) = {
    if (!currentBlock.terminator.isDefined)
      currentBlock.terminator = Some(term)
  }

  //  def call[A <: FirstClassType](returnType: A, name: String, arguments: Seq[Operand]) = {
  //    val functionRef = GlobalReference(returnType, name)
  //    val args = for (a <- arguments) yield (a, Seq())
  //
  //    instruction(returnType, Call(functionRef, args))
  //  }
  //
  //  def alloca(aType: FirstClassType) = {
  //    instruction(aType.pointer, Alloca(aType, None))
  //  }
  //
  //  def load(aType: FirstClassType, address: Operand) = {
  //    instruction(aType, Load(address))
  //  }
  //
  //  def store[A <: Type](address: Operand, value: Operand) = {
  //    instruction(Store(value, address))
  //  }
  //
  //  def ret(returnOperand: Operand) = {
  //    terminator(Ret(Some(returnOperand)))
  //  }
  //
  //  def condBr(condition: Operand, trueDest: String, falseDest: String) = {
  //    terminator(CondBr(condition, trueDest, falseDest))
  //  }
  //
  //  def br(dest: String) = {
  //    terminator(Br(dest))
  //  }
  //
  //  def phi(aType: FirstClassType, values: Seq[(Operand, String)]) = {
  //    instruction(aType, Phi(aType, values))
  //  }
  //
  //  def add(operand0: Operand, operand1: Operand) = {
  //    instruction(int, Add(operand0, operand1))
  //  }
  //
  //  def sub(operand0: Operand, operand1: Operand) = {
  //    instruction(int, Sub(operand0, operand1))
  //  }
  //
  //  def mul(operand0: Operand, operand1: Operand) = {
  //    instruction(int, Mul(operand0, operand1))
  //  }
  //
  //  def div(operand0: Operand, operand1: Operand) = {
  //    instruction(int, SDiv(operand0, operand1))
  //  }
  //
  //  def and(operand0: Operand, operand1: Operand) = {
  //    instruction(int, And(operand0, operand1))
  //  }
  //
  //  def or(operand0: Operand, operand1: Operand) = {
  //    instruction(int, Or(operand0, operand1))
  //  }
  //
  //  def mod(operand0: Operand, operand1: Operand) = {
  //    instruction(int, SRem(operand0, operand1))
  //  }
  //
  //  def fadd(operand0: Operand, operand1: Operand) = {
  //    instruction(float, FAdd(operand0, operand1))
  //  }
  //
  //  def fsub(operand0: Operand, operand1: Operand) = {
  //    instruction(float, FSub(operand0, operand1))
  //  }
  //
  //  def fmul(operand0: Operand, operand1: Operand) = {
  //    instruction(float, FMul(operand0, operand1))
  //  }
  //
  //  def fdiv(operand0: Operand, operand1: Operand) = {
  //    instruction(float, FDiv(operand0, operand1))
  //  }
  //
  //  def icmp(predicate: IntegerPredicate, cast: Boolean = true)(operand0: Operand, operand1: Operand) = {
  //    val i = instruction(Type.Int(1), ICmp(predicate, operand0, operand1))
  //
  //    if (cast)
  //      instruction(int, ZExt(i, int))
  //    else
  //      i
  //  }
  //
  //  def fcmp(predicate: FloatingPointPredicate, cast: Boolean = true)(operand0: Operand, operand1: Operand) = {
  //    val i = instruction(Type.Int(1), FCmp(predicate, operand0, operand1))
  //
  //    if (cast)
  //      instruction(int, ZExt(i, int))
  //    else
  //      i
  //  }
  //
  //  def constantInt(i: Int) = {
  //    Constant.Int(int, i)
  //  }
  //
  //  def constantFloat(f: Float) = {
  //    Constant.Float(float, f)
  //  }
}
