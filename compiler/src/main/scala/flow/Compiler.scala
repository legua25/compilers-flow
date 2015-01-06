package flow

import scala.collection.mutable
import NativeTypes._
import ast._
import llvm.{ Parameter => _, _ }

trait Compiler extends GlobalCodegen
  with NativeTypes
  with DefLookup
  with Scopes {

  def compile(moduleName: String, program: Program): Module = {
    scoped {
      program.statements foreach {
        case FunDef(name, parameters, typeAnn, _) =>
          val Some(pts) = parameters.map(_.map(p => nativeTypeFor(p.aType)))
          val rt = nativeTypeFor(typeAnn)
          scope.declare(name, pts, rt)
        case VarDef(name, typeAnn, _, isMutable) =>
          val t = nativeTypeFor(typeAnn)
          val g = define(
            llvm.GlobalVariable(
              name = name,
              linkage = llvm.Linkage.Private,
              aType = t.toLlvm,
              initializer = Some(Constant.ZeroInitializer)))
          scope.put(Variable(name, t, g, isMutable))
        case _ =>
      }

      program.statements foreach {
        case fd @ FunDef(_, _, _, _) => compile(fd)
        case _                       =>
      }

      setBlock(newBlock("entry"))

      program.statements foreach {
        case fd @ FunDef(_, _, _, _) =>
        case other                   => compile(other)
      }

      terminator(Ret(Some(Constant.Int(llvm.Type.Int(32), "0"))))

      define(
        llvm.Function(
          returnType = llvm.Type.Int(32),
          name = "main",
          basicBlocks = makeBasicBlocks()))
    }

    module(moduleName)
  }

  def compile(statement: Statement): Unit = statement match {
    case FunDef(name, parameters, typeAnn, body) =>
      scoped {
        setBlock(newBlock("entry"))

        val declaredType = nativeTypeFor(typeAnn)

        val llvmParameters =
          for (Parameter(n, ta) <- parameters.get) yield {
            val t = nativeTypeFor(ta)
            val lt = t.toLlvm
            val pt = lt.pointer
            val localName = newLocalName()

            val pointer = instruction(pt, Alloca(lt))
            instruction(Store(LocalReference(lt, localName), pointer))

            scope.put(Variable(n, t, pointer, false))

            llvm.Parameter(lt, localName)
          }

        val (operand, actualType) = compile(body)

        if (actualType != declaredType)
          error(s"Declared result type $declaredType does not match actual type $actualType.")

        terminator(Ret(Some(operand)))

        define(
          llvm.Function(
            returnType = actualType.toLlvm,
            name = name,
            parameters = llvmParameters,
            basicBlocks = makeBasicBlocks()))
      }

    case e: Expression => compile(e)
  }

  def compile(expression: Expression): (Operand, Type) = expression match {
    case VarDef(name, typeAnn, expr, isMutable) =>
      val declaredType = nativeTypeFor(typeAnn)

      if (!scope.isTop) {
        val lt = declaredType.toLlvm
        val pointer = instruction(lt.pointer, Alloca(lt))
        scope.put(Variable(name, declaredType, pointer, isMutable))
      }

      val (operand, actualType) = compile(expr)

      if (actualType != declaredType)
        error(s"Declared type $declaredType does not match actual type $actualType.")

      val Variable(_, _, pointer, _) = scope.variableFor(name)
      instruction(Store(operand, pointer))

      (unit, Unit)

    case If(condition, thn, els) =>
      val entryBlock = currentBlock
      val thenBlock = newBlock("then")
      val elseBlock = newBlock("else")
      val continueBlock = newBlock("continue")

      val (cond, condType) = compile(condition)

      if (condType != Bool)
        error(s"Condition evaluated to $condType, expected $Bool.")

      setBlock(thenBlock)
      val (e0, t0) = compile(thn)

      setBlock(elseBlock)
      val (e1, t1) = els match {
        case Some(expr) =>
          val (e1, t1) = compile(expr)
          (e1, t1)
        case None =>
          (unit, Unit)
      }

      // TODO: type unification
      if (t0 != t1)
        error(s"Type $t0 does not match type $t1.")

      setBlock(entryBlock)
      val lt = t0.toLlvm
      val resPointer = instruction(lt.pointer, Alloca(lt))
      terminator(CondBr(cond, thenBlock, elseBlock))

      setBlock(thenBlock)
      instruction(Store(e0, resPointer))
      terminator(Br(continueBlock))

      setBlock(elseBlock)
      instruction(Store(e1, resPointer))
      terminator(Br(continueBlock))

      setBlock(continueBlock)
      val result = instruction(lt, Load(resPointer))

      (result, t0)

    case While(condition, body) =>
      val conditionBlock = newBlock("condition")
      val loopBlock = newBlock("loop")
      val continueBlock = newBlock("continue")

      terminator(Br(conditionBlock))

      setBlock(conditionBlock)
      val (cond, condType) = compile(condition)

      if (condType != Bool)
        error(s"Condition evaluated to $condType, expected $Bool.")

      terminator(CondBr(cond, loopBlock, continueBlock))

      setBlock(loopBlock)
      compile(body)
      terminator(Br(conditionBlock))

      setBlock(continueBlock)

      (unit, Unit)

    case Block(expressions) =>
      scoped {
        expressions.foldLeft((unit: Operand, Unit: Type)) {
          case (_, e) => compile(e)
        }
      }

    case InfixExpression(e0, op, e1) =>
      val (v0, t0) = compile(e0)
      val (v1, t1) = compile(e1)

      defFor(t0, op, Some(Seq(t1))) match {
        case NativeFunDef(_, _, rt, body) => (body(Seq(v0, v1)), rt)
      }

    case Id(name) =>
      val Variable(_, aType, pointer, _) = scope.variableFor(name)
      (instruction(aType.toLlvm, Load(pointer)), aType)

    case Application(Id("print"), Seq(argument)) =>
      val (v, t) = compile(argument)

      defFor(t, "print", Some(Seq())) match {
        case NativeFunDef(_, _, rt, body) => (body(Seq(v)), rt)
      }

    case Application(Id(funName), arguments) =>
      val (args, argTypes) =
        (for (arg <- arguments) yield {
          val (v, t) = compile(arg)
          ((v, Seq()), t)
        }).unzip

      val Function(_, _, resultType, ref) = scope.functionFor(funName, argTypes)

      val result = instruction(resultType.toLlvm, Call(ref, args))

      (result, resultType)

    case Application(Selection(expr, name), arguments) =>
      val (e, t) = compile(expr)

      val (args, argTypes) = arguments.map(compile).unzip

      val NativeFunDef(_, _, resultType, body) = defFor(t, name, Some(argTypes))

      val result = body(e +: args)

      (result, resultType)

    case Assignment(Id(name), expr) =>
      val Variable(_, declaredType, pointer, isMutable) = scope.variableFor(name)
      if (!isMutable)
        error(s"Cannot reassign into val $name.")

      val (o, actualType) = compile(expr)

      if (actualType != declaredType)
        error(s"Cannot assign type $actualType to $declaredType.")

      instruction(Store(o, pointer))

      (unit, Unit)

    case BoolLiteral(value)   => (constantBool(value), Bool)
    case CharLiteral(value)   => (constantChar(value), Char)
    case StringLiteral(value) => ???
    case IntLiteral(value)    => (constantInt(value), Int)
    case FloatLiteral(value)  => (constantFloat(value), Float)
    case Parenthesized(e)     => compile(e)
    case e                    => println(e); ???
  }

  def typeOfId(name: String) =
    scope.variableFor(name).aType

  def typeDefOf(aType: Type) =
    nativeTypes(aType)

}
