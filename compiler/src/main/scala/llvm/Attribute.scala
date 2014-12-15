package llvm

/**
 * http://llvm.org/docs/LangRef.html#parameter-attributes
 */
sealed abstract class ParameterAttribute(val llvm: String)

object ParameterAttribute {

  case object ZeroExt extends ParameterAttribute("zeroext")

  case object SignExt extends ParameterAttribute("signext")

  case object InReg extends ParameterAttribute("inreg")

  case object ByVal extends ParameterAttribute("byval")
  
  case object InAlloca extends ParameterAttribute("inalloca")

  case object SRet extends ParameterAttribute("sret")
  
  case class Align(n: Int) extends ParameterAttribute(s"align $n")

  case object NoAlias extends ParameterAttribute("noalias")

  case object NoCapture extends ParameterAttribute("nocapture")

  case object Nest extends ParameterAttribute("nest")

  case object Returned extends ParameterAttribute("returned")
  
  case object NoNull extends ParameterAttribute("nonull")
  
  case class Dereferenceable(n: Int) extends ParameterAttribute(s"dereferenceable($n)")

}

/**
 * http://llvm.org/docs/LangRef.html#function-attributes
 */
sealed abstract class FunctionAttribute(val llvm: String)

object FunctionAttribute {

  case class AlignStack(n: Int) extends FunctionAttribute(s"alignstack($n)")

  case object AlwaysInline extends FunctionAttribute("alwaysinline")
  
  case object BuiltIn extends FunctionAttribute("builtin")
  
  case object Cold extends FunctionAttribute("cold")

  case object InlineHint extends FunctionAttribute("inlinehint")
  
  case object JumpTable extends FunctionAttribute("jumptable")
  
  case object MinSize extends FunctionAttribute("minsize")
  
  case object Naked extends FunctionAttribute("naked")
  
  case object NoBuiltIn extends FunctionAttribute("nobuiltin")
  
  case object NoDuplicate extends FunctionAttribute("noduplicate")
  
  case object NoImplicitFloat extends FunctionAttribute("noimplicitfloat")
  
  case object NoInline extends FunctionAttribute("noinline")
  
  case object NonLazyBind extends FunctionAttribute("nolazybind")
  
  case object NoRedZone extends FunctionAttribute("noredzone")

  case object NoReturn extends FunctionAttribute("noreturn")

  case object NoUnwind extends FunctionAttribute("nounwind")
  
  case object OptimizeNone extends FunctionAttribute("optnone")
  
  case object OptimizeSize extends FunctionAttribute("optsize")

  case object ReadNone extends FunctionAttribute("readnone")

  case object ReadOnly extends FunctionAttribute("readonly")
  
  case object ReturnsTwice extends FunctionAttribute("returns_twice")
  
  case object SanitizeAddress extends FunctionAttribute("sanitize_address")

  case object SanitizeMemory extends FunctionAttribute("sanitize_memory")
  
  case object SanitizeThread extends FunctionAttribute("sanitize_thread")
  
  case object StackProtect extends FunctionAttribute("ssp")

  case object StackProtectReq extends FunctionAttribute("sspreq")
  
  case object StackProtectStrong extends FunctionAttribute("sspstrong")

  case object UWTable extends FunctionAttribute("uwtable")

}
