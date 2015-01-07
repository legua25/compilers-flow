package flow

import scala.collection.mutable
import NativeTypes._
import ast._
import llvm.{ Parameter => _, _ }

trait Compiler
  extends GlobalCodegen
  with BlockCodegen
  with NativeTypes
  with DefLookup
  with Scopes {

  def compile(moduleName: String, program: Program): Module = {
    scoped {
      program.statements foreach {
        case Definition(name, parameters, typeAnn, _) =>
          val Some(pts) = parameters.map(_.map(p => nativeTypeFor(p.aType)))
          val lps = pts.map(t => llvm.Parameter(t.toLlvm))
          val rt = nativeTypeFor(typeAnn)
          val ref = define(
            llvm.Function(
              returnType = rt.toLlvm,
              name = name,
              parameters = lps))
          scope.put(Function(name, pts, rt, ref))
        case VarDefinition(name, typeAnn, _, isMutable) =>
          val t = nativeTypeFor(typeAnn)
          val ref = define(
            llvm.GlobalVariable(
              name = name,
              linkage = llvm.Linkage.Private,
              aType = t.toLlvm,
              initializer = Some(Constant.ZeroInitializer)))
          scope.put(Variable(name, t, isMutable, ref))
        case _ =>
      }

      program.statements foreach {
        case fd @ Definition(_, _, _, _) => compile(fd)
        case _                           =>
      }

      setBlock(newBlock("entry"))

      program.statements foreach {
        case fd @ Definition(_, _, _, _) =>
        case other                       => compile(other)
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
    case Definition(name, parameters, typeAnn, body) =>
      scoped {
        setBlock(newBlock("entry"))

        val declaredType = nativeTypeFor(typeAnn)

        val llvmParameters =
          for (Parameter(n, ta) <- parameters.get) yield {
            val t = nativeTypeFor(ta)
            val lt = t.toLlvm
            val pt = lt.pointer
            val localName = newLocalName()

            val ref = instruction(pt, Alloca(lt))
            instruction(Store(LocalReference(lt, localName), ref))

            scope.put(Variable(n, t, false, ref))

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
    case VarDefinition(name, typeAnn, expr, isMutable) =>
      val declaredType = nativeTypeFor(typeAnn)

      if (!scope.isTop) {
        val lt = declaredType.toLlvm
        val ref = instruction(lt.pointer, Alloca(lt))
        scope.put(Variable(name, declaredType, isMutable, ref))
      }

      val (operand, actualType) = compile(expr)

      if (actualType != declaredType)
        error(s"Declared type $declaredType does not match actual type $actualType.")

      val Variable(_, _, _, ref) = scope.variableFor(name)
      instruction(Store(operand, ref))

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
      val thenContinueBlock = currentBlock

      setBlock(elseBlock)
      val (e1, t1) = els match {
        case Some(expr) =>
          val (e1, t1) = compile(expr)
          (e1, t1)
        case None =>
          (unit, Unit)
      }
      val elseContinueBlock = currentBlock

      // TODO: type unification
      if (t0 != t1)
        error(s"Type $t0 does not match type $t1.")

      setBlock(entryBlock)
      val lt = t0.toLlvm
      val resRef = instruction(lt.pointer, Alloca(lt))
      terminator(CondBr(cond, thenBlock, elseBlock))

      setBlock(thenContinueBlock)
      instruction(Store(e0, resRef))
      terminator(Br(continueBlock))

      setBlock(elseContinueBlock)
      instruction(Store(e1, resRef))
      terminator(Br(continueBlock))

      setBlock(continueBlock)
      val result = instruction(lt, Load(resRef))

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
      val Variable(_, aType, _, ref) = scope.variableFor(name)
      (instruction(aType.toLlvm, Load(ref)), aType)

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
      val Variable(_, declaredType, isMutable, ref) = scope.variableFor(name)
      if (!isMutable)
        error(s"Cannot reassign into val $name.")

      val (o, actualType) = compile(expr)

      if (actualType != declaredType)
        error(s"Cannot assign type $actualType to $declaredType.")

      instruction(Store(o, ref))

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
