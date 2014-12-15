package llvm

sealed abstract class DllStorageClass(val llvm: String)

object DllStorageClass {

  case object DllImport extends DllStorageClass("dllimport")

  case object DllExport extends DllStorageClass("dllexport")

}
