package flow

import ast._

import llvm.Module

trait Compiler extends GlobalCodegen {
  // todo:
  def compile(moduleName: String, program: Program): Module = ???
}
