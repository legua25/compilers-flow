package flow

import scala.collection.mutable
import llvm.{ Operand, GlobalReference }
import llvm.GlobalReference

trait Scopes {

  sealed trait CompiledDef

  case class Variable(
    name: String,
    aType: Type,
    isMutable: Boolean,
    pointer: Operand) extends CompiledDef

  case class Function(
    name: String,
    parameterTypes: Seq[Type],
    resultType: Type,
    reference: GlobalReference) extends CompiledDef

  type Scope = mutable.Map[Signature, CompiledDef]

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

    def assertNotDefined(name: String) = {
      if (current.contains(name))
        error(s"Variable $name is already defined.")
    }

    def assertNotDefined(name: String, argumentTypes: Seq[Type]) = {
      if (current.contains(name)) {
        val argTypes = argumentTypes.mkString("(", ",", ")")
        error(s"Function $name($argTypes) is already defined.")
      }
    }

    def memberFor(
      signature: Signature,
      scopes: List[Scope] = this.scopes): Option[(CompiledDef, Scope)] = {

      if (scopes.isEmpty) {
        None
      }
      else {
        scopes.head.get(signature) match {
          case Some(m) => Some((m, scopes.head))
          case None    => memberFor(signature, scopes.tail)
        }
      }
    }

    def variableFor(name: String) = memberFor(name) match {
      case Some((v: Variable, _)) => v
      case Some((a, _))           => illegal(s"${a.getClass().getName()} is not a Variable.")
      case None                   => error(s"Variable $name is not defined.")
    }

    def functionFor(name: String, argumentTypes: Seq[Type]) = memberFor((name, argumentTypes)) match {
      case Some((f: Function, _)) => f
      case Some((a, _))           => illegal(s"${a.getClass().getName()} is not a Function.")
      case None =>
        val argTypes = argumentTypes.mkString("(", ",", ")")
        error(s"Function $name($argTypes) is not defined.")
    }

    def put(variable: Variable) = {
      assertNotDefined(variable.name)
      current(variable.name) = variable
    }

    def put(function: Function) = {
      assertNotDefined(function.name, function.parameterTypes)
      current((function.name, function.parameterTypes)) = function
    }

    implicit def nameToSignature(name: String): Signature =
      Signature(name, None)

    implicit def pairToSignature(pair: (String, Seq[Type])): Signature =
      Signature(pair._1, Some(pair._2))

  }

}
