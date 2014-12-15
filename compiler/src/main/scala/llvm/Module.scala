package llvm

case class Module(name: String, definitions: Seq[Definition]) {
  def llvm: String = s"; ModuleID = '$name'\n\n" + definitions.map(_.llvm).mkString("\n\n")
}
