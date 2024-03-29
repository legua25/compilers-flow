package flow

import scala.collection.mutable

import llvm._

class BlockState(val name: String) {

  val instructions = mutable.ListBuffer.empty[(Option[String], Instruction)]
  var terminator = Option.empty[Terminator]

  def basicBlock = {
    if (terminator.isEmpty)
      error(s"Unterminated BasicBlock: $name")

    BasicBlock(name, instructions.toList, terminator.getOrElse(Br(name)))
  }

}

trait BlockCodegen extends LlvmNames {

  private val blockFor = mutable.Map.empty[String, BlockState]
  private val blocks = mutable.ListBuffer.empty[BlockState]

  private var _currentBlock: BlockState = _

  private var nrOfLocals = 0

  def newLocalName() = {
    val n = nrOfLocals
    nrOfLocals += 1
    s".$n"
  }

  private def blockNameFrom(name0: String) = {
    val safeName = safeNameFrom(name0)

    var name: String = safeName
    var i = 1

    while (blockFor.isDefinedAt(name)) {
      name = name0 + i
      i += 1
    }

    name
  }

  def currentBlock =
    _currentBlock.name

  def newBlock(name0: String) = {
    val name = blockNameFrom(name0)
    val block = new BlockState(name)
    blockFor(name) = block
    blocks += block
    name
  }

  def setBlock(name: String) = {
    if (!blockFor.isDefinedAt(name))
      error(s"Undefined block: $name")

    _currentBlock = blockFor(name)
  }

  def makeBasicBlocks() = {
    val result = blocks.map(_.basicBlock).toList

    blockFor.clear()
    blocks.clear()
    _currentBlock = null
    nrOfLocals = 0

    result
  }

  def instruction(instr: Instruction): Unit = {
    _currentBlock.instructions += ((None, instr))
  }

  def instruction(aType: llvm.Type, instr: Instruction) = {
    val n = newLocalName()

    if (_currentBlock.terminator.isEmpty)
      _currentBlock.instructions += ((Some(n), instr))

    LocalReference(aType, n)
  }

  def terminator(term: Terminator) = {
    if (_currentBlock.terminator.isDefined)
      error(s"${_currentBlock.name} is already terminated.")

    _currentBlock.terminator = Some(term)
  }

  // Aliases ===================================================================

  def alloca(aType: llvm.Type) = {
    instruction(aType.pointer, Alloca(aType))
  }

  def store(value: Operand, address: Operand) =
    instruction(Store(value, address))

  def load(aType: llvm.Type, address: Operand) =
    instruction(aType, Load(address))

  def call(aType: llvm.Type, function: Operand, arguments: Seq[Operand]) =
    instruction(aType, Call(function, arguments.map(a => (a, Seq()))))

  def call(function: Operand, arguments: Seq[Operand]) =
    instruction(Call(function, arguments.map(a => (a, Seq()))))

  def br(condition: Operand, trueDest: Name, falseDest: Name) =
    terminator(CondBr(condition, trueDest, falseDest))

  def br(dest: Name) =
    terminator(Br(dest))

  def ret() =
    terminator(Ret(None))

  def ret(value: Operand) =
    terminator(Ret(Some(value)))
}
