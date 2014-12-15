package object llvm {

  type Name = String

  /**
   * http://llvm.org/docs/LangRef.html#pointer-type
   */
  case class AddrSpace(n: Int) {
    def llvm: String = s"addrspace($n)"
  }
  
}
