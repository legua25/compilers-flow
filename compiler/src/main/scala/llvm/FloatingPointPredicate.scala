package llvm

/**
 * http://llvm.org/docs/LangRef.html#fcmp-instruction
 */
sealed abstract class FloatingPointPredicate(val llvm: String)

object FloatingPointPredicate {
  
  case object True extends FloatingPointPredicate("true")
  
  case object False extends FloatingPointPredicate("false")

  case object OEQ extends FloatingPointPredicate("oeq")

  case object OGT extends FloatingPointPredicate("ogt")

  case object OGE extends FloatingPointPredicate("oge")

  case object OLT extends FloatingPointPredicate("olt")

  case object OLE extends FloatingPointPredicate("ole")

  case object ONE extends FloatingPointPredicate("one")

  case object ORD extends FloatingPointPredicate("ord")

  case object UNO extends FloatingPointPredicate("uno")

  case object UEQ extends FloatingPointPredicate("ueq")

  case object UGT extends FloatingPointPredicate("ugt")

  case object UGE extends FloatingPointPredicate("uge")

  case object ULT extends FloatingPointPredicate("ult")

  case object ULE extends FloatingPointPredicate("ule")

  case object UNE extends FloatingPointPredicate("une")

}
