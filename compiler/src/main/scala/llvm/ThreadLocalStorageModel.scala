package llvm

sealed abstract class ThreadLocalStorageModel(val model: String) {
  def llvm = s"thread_local($model)"
}

object ThreadLocalStorageModel {
  
  case object LocalDynamic extends ThreadLocalStorageModel("localdynamic")

  case object InitialExec extends ThreadLocalStorageModel("initialexec")
  
  case object LocalExec extends ThreadLocalStorageModel("localexec")
  
}
