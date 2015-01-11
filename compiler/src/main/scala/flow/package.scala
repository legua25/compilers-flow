import java.io.File

package object flow {

  var debugMode = false

  def debug(message: Any) =
    if (debugMode) println(message)

  def error(message: String) =
    throw new CompilerException(message)

  def illegal(message: String) =
    throw new IllegalStateException(message)

  def ??? =
    throw new NotImplementedError("Feature is not implemented yet.")

}

package flow {

  class CompilerException(message: String) extends RuntimeException(message)

}
