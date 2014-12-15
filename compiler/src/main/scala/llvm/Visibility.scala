package llvm

/**
 * http://llvm.org/docs/LangRef.html#visibility-styles
 */
sealed abstract class Visibility(val llvm: String)

object Visibility {

  case object Default extends Visibility("default")

  case object Hidden extends Visibility("hidden")

  case object Protected extends Visibility("protected")

}
