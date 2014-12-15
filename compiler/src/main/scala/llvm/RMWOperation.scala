package llvm

/**
 * http://llvm.org/docs/LangRef.html#atomicrmw-instruction
 */
sealed abstract class RMWOperation(val llvm: String)

object RMWOperation {

  case object Xchg extends RMWOperation("xchg")

  case object Add extends RMWOperation("add")

  case object Sub extends RMWOperation("sub")

  case object And extends RMWOperation("and")

  case object Nand extends RMWOperation("nand")

  case object Or extends RMWOperation("or")

  case object Xor extends RMWOperation("xor")

  case object Max extends RMWOperation("max")

  case object Min extends RMWOperation("min")

  case object UMax extends RMWOperation("umax")

  case object UMin extends RMWOperation("umin")

}
