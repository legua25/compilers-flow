package flow

import scala.collection.mutable

import flow.{ Type => _ }
import llvm._

trait GlobalCodegen extends LlvmNames {

  private val definitionList = mutable.ListBuffer.empty[Definition]

  private val declaredFunctions = mutable.Map.empty[GlobalReference, Int]

  private var nrOfGlobals = 0

  def newGlobalName() = {
    val n = nrOfGlobals
    nrOfGlobals += 1
    s".$n"
  }

  def declare(
    returnType: llvm.Type,
    qualifiedName: QualifiedName,
    parameters: Seq[llvm.Parameter]) = {

    val name = qualifiedName.parts.map(safeNameFrom).mkString("_")

    val function =
      Function(
        returnType = returnType,
        name = name,
        parameters = parameters)

    val reference = GlobalReference(returnType, name)

    declaredFunctions(reference) = definitionList.size
    definitionList += GlobalDefinition(function)

    reference
  }

  def define(
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

  def defineInternal(function: Function) = {
    definitionList += GlobalDefinition(function)
    GlobalReference(function.returnType, function.name)
  }

  def define(variable: GlobalVariable) = {
    ???
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

  case class QualifiedName(parts: String*)

  object QualifiedName {

    def apply(part0: String, parts: String*): QualifiedName =
      QualifiedName(part0 +: parts: _*)

    def apply(part0: String, part1: String, parts: String*): QualifiedName =
      QualifiedName(part0 +: part1 +: parts: _*)

    def apply(part0: String, part1: String, part2: String, parts: String*): QualifiedName =
      QualifiedName(part0 +: part1 +: part2 +: parts: _*)

  }

}
