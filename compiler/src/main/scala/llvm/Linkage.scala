package llvm

/**
 * http://llvm.org/docs/LangRef.html#linkage-types
 */
sealed abstract class Linkage(val llvm: String)

object Linkage {

  case object Private extends Linkage("private")

  case object Internal extends Linkage("internal")

  case object AvailableExternally extends Linkage("available_externally")

  case object LinkOnce extends Linkage("linkonce")

  case object Weak extends Linkage("weak")

  case object Common extends Linkage("common")

  case object Appending extends Linkage("appending")

  case object ExternWeak extends Linkage("extern_weak")

  case object LinkOnceODR extends Linkage("linkonce_odr")

  case object WeakODR extends Linkage("weak_odr")

  case object External extends Linkage("external")

}
