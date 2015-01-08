package flow

import scala.collection.mutable
import llvm.{ Operand, GlobalReference }
import llvm.GlobalReference

trait Scopes {

  type Scope = mutable.Map[Signature, Def]

  object Scope { def apply(): Scope = mutable.Map() }

  val scope = new Scopes

  def scoped[A](block: => A): A = {
    scope.pushNew()
    val result = block
    scope.pop()
    result
  }

  class Scopes {

    private var scopes = List.empty[Scope]

    def pushNew() = scopes ::= Scope()

    def pop() = scopes = scopes.tail

    def current = scopes.head

    def isTop = scopes.tail.isEmpty

    def assertNotDefined(defn: Def) = {
      if (current.contains(defn.signature))
        error(s"${defn.signature} is already defined.")
    }

    def find(
      signature: Signature,
      scopes: List[Scope] = this.scopes): Option[(Def, Scope)] = {

      if (scopes.isEmpty) {
        None
      }
      else {
        scopes.head.get(signature) match {
          case Some(m) => Some((m, scopes.head))
          case None    => find(signature, scopes.tail)
        }
      }
    }

    def defFor(signature: Signature): Def = find(signature) match {
      case Some((defn, _)) => defn
      case None            => error(s"$signature is not defined.")
    }

    def defFor(name: String): Def =
      defFor(Signature(name, None))

    def defFor(name: String, parameterTypes: Option[Seq[Type]]): Def =
      defFor(Signature(name, parameterTypes))

    def put(defn: Def) = {
      assertNotDefined(defn)
      current(defn.signature) = defn
    }

  }

}
