package llvm

/**
 * http://llvm.org/docs/LangRef.html#icmp-instruction
 */
sealed abstract class IntegerPredicate(val llvm: String)

object IntegerPredicate {

  case object EQ extends IntegerPredicate("eq")

  case object NE extends IntegerPredicate("ne")

  case object UGT extends IntegerPredicate("ugt")

  case object UGE extends IntegerPredicate("uge")

  case object ULT extends IntegerPredicate("ult")

  case object ULE extends IntegerPredicate("ule")

  case object SGT extends IntegerPredicate("sgt")

  case object SGE extends IntegerPredicate("sge")

  case object SLT extends IntegerPredicate("slt")

  case object SLE extends IntegerPredicate("sle")

}
