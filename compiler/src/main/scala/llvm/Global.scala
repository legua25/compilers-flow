package llvm

/**
 * http://llvm.org/doxygen/classllvm_1_1GlobalValue.html
 */
sealed trait Global {
  def llvm: String
}

/**
 * http://llvm.org/docs/LangRef.html#global-variables
 */
case class GlobalVariable(
  name: Name,
  linkage: Linkage = Linkage.External,
  visibility: Visibility = Visibility.Default,
  dllStorageClass: Option[DllStorageClass] = None,
  threadLocal: Option[ThreadLocalStorageModel] = None,
  hasUnnamedAddr: Boolean = false,
  addrSpace: AddrSpace = AddrSpace(0),
  isInitializedExternally: Boolean = false,
  isConstant: Boolean = false,
  aType: Type,
  initializer: Option[Constant] = None,
  section: Option[String] = None,
  alignment: Int = 0) extends Global {

  def llvm = {
    s"@$name = " + Seq(
      linkage.llvm,
      visibility.llvm,
      dllStorageClass.map(_.llvm).getOrElse(""),
      threadLocal.map(_.llvm).getOrElse(""),
      if (hasUnnamedAddr) "unnamed_addr" else "",
      if (addrSpace != AddrSpace(0)) addrSpace.llvm else "",
      if (isInitializedExternally) "external" else "",
      if (isConstant) "constant" else "global",
      aType.llvm,
      initializer.map(_.repr).getOrElse(""),
      section.map(s => ", section \"" + s + "\"").getOrElse(""),
      if (alignment != 0) s", align $alignment" else "").filter(_ != "").mkString(" ")
  }
}

/**
 * http://llvm.org/docs/LangRef.html#aliases>
 */
case class GlobalAlias(
  name: Name,
  linkage: Linkage = Linkage.External,
  visibility: Visibility = Visibility.Default,
  dllStorageClass: Option[DllStorageClass] = None,
  threadLocal: Option[ThreadLocalStorageModel] = None,
  hasUnnamedAddr: Boolean = false,
  aType: Type,
  aliasee: Constant) extends Global {

  def llvm = {
    s"@$name = " + Seq(
      linkage.llvm,
      visibility.llvm,
      dllStorageClass.map(_.llvm).getOrElse(""),
      threadLocal.map(_.llvm).getOrElse(""),
      if (hasUnnamedAddr) "unnamed_addr" else "",
      "alias",
      aType.llvm,
      aliasee.repr).filter(_ != "").mkString(" ")
  }
}

/**
 * http://llvm.org/docs/LangRef.html#functions
 */
case class Function(
  linkage: Option[Linkage] = None,
  visibility: Option[Visibility] = None,
  dllStorageClass: Option[DllStorageClass] = None,
  callingConvention: Option[CallingConvention] = None,
  returnAttributes: Seq[ParameterAttribute] = Seq(),
  returnType: Type,
  name: Name,
  parameters: Seq[Parameter] = Seq(),
  isVariadic: Boolean = false,
  hasUnnamedAddr: Boolean = false,
  functionAttributes: Seq[FunctionAttribute] = Seq(),
  section: Option[String] = None,
  alignment: Int = 0,
  garbageCollectorName: Option[String] = None,
  basicBlocks: Seq[BasicBlock] = Seq()) extends Global {

  def llvm = {
    val declare = basicBlocks.isEmpty

    def llvmSeq(p: Parameter) =
      if (declare) Seq(p.aType.llvm)
      else Seq(p.aType.llvm, "%" + p.name)

    val params = parameters map {
      p =>
        (llvmSeq(p) ++ p.attributes.map(_.llvm))
          .filter(_ != "").mkString(" ")
    }

    val paramClause = (params :+ (if (isVariadic) "..." else ""))
      .filter(_ != "").mkString("(", ", ", ")")

    val blocks = (for (BasicBlock(n, is, t) <- basicBlocks) yield {
      Seq(
        s"$n:",
        (is map {
          case (Some(n), i) => s"  %$n = ${i.llvm}"
          case (None, i)    => s"  ${i.llvm}"
        }).mkString("\n"),
        s"  ${t.llvm}").filter(_ != "").mkString("\n")
    }).mkString("\n\n")

    val body = if (declare)
      ""
    else
      Seq("{", blocks, "}").filter(_ != "").mkString("\n")

    Seq(
      if (declare) "declare" else "define",
      linkage.map(_.llvm).getOrElse(""),
      visibility.map(_.llvm).getOrElse(""),
      dllStorageClass.map(_.llvm).getOrElse(""),
      callingConvention.map(_.llvm).getOrElse(""),
      returnAttributes.map(_.llvm).mkString(" "),
      returnType.llvm,
      s"@$name" + paramClause,
      if (hasUnnamedAddr) "unnamed_addr" else "",
      functionAttributes.map(_.llvm).mkString(" "),
      section.map(s => ", section \"" + s + "\"").getOrElse(""),
      if (alignment != 0) s", align $alignment" else "",
      garbageCollectorName.getOrElse(""),
      body).filter(_ != "").mkString(" ")
  }
}

case class Parameter(aType: FirstClassType, name: Name, attributes: Seq[ParameterAttribute] = Seq())

case class BasicBlock(
  name: Name,
  instructions: Seq[(Option[Name], Instruction)],
  terminator: Terminator)
