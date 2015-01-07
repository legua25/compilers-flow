package flow

import scala.collection.mutable

import llvm._

trait GlobalCodegen {

  private val globals = mutable.ListBuffer.empty[Global]

  type Key = (String, Seq[llvm.Type])
  private val definitionMap = mutable.Map.empty[Key, Int]
  private val names = mutable.Set.empty[String]

  private var nrOfGlobals = 0

  def newGlobalName() = {
    val n = nrOfGlobals
    nrOfGlobals += 1
    s".$n"
  }

  def uniqueNameFrom(name0: String) = {
    var name: String = name0
    var i = 1

    while (names.contains(name)) {
      name = name0 + i
      i += 1
    }

    name
  }

  def define(function0: llvm.Function) = {
    val key = (function0.name, function0.parameters.map(_.aType))
    val idx = definitionMap.get(key)
    val name = idx
      .map(globals)
      .collect({ case f: llvm.Function => f.name })
      .getOrElse(uniqueNameFrom(function0.name))
    val function = function0.copy(name = name)

    idx match {
      case Some(idx) =>
        globals(idx) = function
      case None =>
        val idx = globals.size
        definitionMap(key) = idx
        globals += function
        names += function.name
    }

    GlobalReference(function.returnType, function.name)
  }

  def define(variable: GlobalVariable) = {
    globals += variable
    GlobalReference(variable.aType.pointer, variable.name)
  }

  def definitions = {
    val sortedDefinitions = globals sortBy {
      //      case _: TypeDefinition                                      => 0
      case _: GlobalAlias                       => 1
      case _: GlobalVariable                    => 2
      case f: Function if f.basicBlocks.isEmpty => 3
      case _: Function                          => 4
    }

    sortedDefinitions.toList.map(GlobalDefinition)
  }

  def module(name: String) =
    Module(name, definitions)

}
