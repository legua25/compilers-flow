package flow.mirror

import flow.error
import scala.collection.mutable

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

  def defFor(name: String, parameterTypes: Option[Seq[Type]]): Option[Def] =
    scope.find(Signature(name, parameterTypes)).map(_._1)

  def defFor(name: String): Option[Def] =
    defFor(name, None)

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

    def put(defn: Def) = {
      assertNotDefined(defn)
      current(defn.signature) = defn
    }

  }

}
