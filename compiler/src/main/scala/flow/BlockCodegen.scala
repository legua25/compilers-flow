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

  def makeBasicBlocks() = {
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

  def instruction(aType: Type, instr: Instruction) = {
    val n = newUnnamed()

    if (currentBlock.terminator.isEmpty)
      currentBlock.instructions += ((Some(n), instr))

    LocalReference(aType, n)
  }

  def terminator(term: Terminator) = {
    if (!currentBlock.terminator.isDefined)
      currentBlock.terminator = Some(term)
  }
}
