package flow

import scala.collection.mutable

import llvm._

trait GlobalCodegen extends BlockCodegen {
  val definitionList = mutable.ListBuffer.empty[Definition]

  def define(function: Function) = {
    definitionList += GlobalDefinition(function)
    GlobalReference(function.returnType, function.name)
  }

  def definitions = {
    val sortedDefinitions = definitionList sortBy {
      case _: TypeDefinition                                      => 0
      case GlobalDefinition(_: GlobalAlias)                       => 1
      case GlobalDefinition(_: GlobalVariable)                    => 2
      case GlobalDefinition(f: Function) if f.basicBlocks.isEmpty => 3
      case GlobalDefinition(_: Function)                          => 4
    }

    sortedDefinitions.toList
  }

  def module(name: String) =
    Module(name, definitions)
}
