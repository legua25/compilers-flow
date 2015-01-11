package flow

import scala.collection.mutable

import llvm._

trait GlobalCodegen extends LlvmNames {

  // TODO: clean up this variadic shit
  case class QualifiedName(parts: String*) { require(!parts.isEmpty) }

  private val definitionList = mutable.ListBuffer.empty[Definition]

  private val declaredFunctions = mutable.Map.empty[GlobalReference, Int]

  private var nrOfGlobals = 0

  def newGlobalName() = {
    val n = nrOfGlobals
    nrOfGlobals += 1
    s".$n"
  }

  def global_declareUnsafe(
    returnType: llvm.Type,
    name: String,
    parameterTypes: Seq[llvm.Type]) = {

    val function =
      Function(
        returnType = returnType,
        name = name,
        parameters = parameterTypes.map(t => llvm.Parameter(t)))

    val reference = GlobalReference(returnType, name)

    declaredFunctions(reference) = definitionList.size
    definitionList += GlobalDefinition(function)

    reference
  }

  def global_declare(
    returnType: llvm.Type,
    qualifiedName: QualifiedName,
    parameterTypes: Seq[llvm.Type]) = {

    val name = qualifiedName.parts.map(safeNameFrom).mkString("_")
    global_declareUnsafe(returnType, name, parameterTypes)
  }

  def global_define(
    reference: GlobalReference,
    parameters: Seq[llvm.Parameter],
    basicBlocks: Seq[BasicBlock]) = {

    if (!declaredFunctions.contains(reference))
      error(s"${reference.repr} is not declared.")

    val GlobalReference(returnType, name) = reference

    val function =
      Function(
        returnType = returnType,
        name = name,
        parameters = parameters,
        basicBlocks = basicBlocks)

    definitionList(declaredFunctions(reference)) = GlobalDefinition(function)
  }

  def global_defineInternal(function: Function) = {
    definitionList += GlobalDefinition(function)
    GlobalReference(function.returnType, function.name)
  }

  def global_define(variable: GlobalVariable) = {
    definitionList += GlobalDefinition(variable)
    GlobalReference(variable.aType.pointer, variable.name)
  }

  def global_define(typeDefinition: TypeDefinition) = {
    definitionList += typeDefinition
  }

  def definitions = {
    definitionList sortBy {
      case _: TypeDefinition                                      => 0
      case GlobalDefinition(_: GlobalAlias)                       => 1
      case GlobalDefinition(f: Function) if f.basicBlocks.isEmpty => 2
      case GlobalDefinition(_: GlobalVariable)                    => 3
      case GlobalDefinition(_: Function)                          => 4
    }
  }

  def module(name: String) =
    Module(name, definitions)

}
