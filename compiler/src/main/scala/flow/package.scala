package object flow {
  def error(message: String) =
    throw new CompilerException(message)
}

package flow {
  class CompilerException(message: String) extends RuntimeException(message)
}