package llvm

/**
 * http://llvm.org/docs/LangRef.html#callingconv
 */
sealed abstract class CallingConvention(val llvm: String)

object CallingConvention {

  case object C extends CallingConvention("ccc")

  case object Fast extends CallingConvention("fastcc")

  case object Cold extends CallingConvention("coldcc")

  case object GHC extends CallingConvention("cc 10")
  
  case object HiPE extends CallingConvention("cc 11")
  
  case object WebKit extends CallingConvention("webkit_jscc")
  
  case object AnyReg extends CallingConvention("anyregcc")
  
  case object PreserveMost extends CallingConvention("preserve_mostcc")
  
  case object PreserveAll extends CallingConvention("preserve_allcc")

  case class Numbered(n: Int) extends CallingConvention(s"cc $n")

}
