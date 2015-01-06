package object flow {
  def error(message: String) =
    throw new CompilerException(message)

  def illegal(message: String) =
    throw new IllegalStateException(message)
}

package flow {
  class CompilerException(message: String) extends RuntimeException(message)
}
