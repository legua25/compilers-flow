package flow

import scala.collection.mutable
import NativeTypes._
import flow.{ syntax => ast }

trait Compiler
  extends GlobalCodegen
  with BlockCodegen
  with NativeTypes
  with Types
  with Scopes {

  def typeOfId(name: String) = {
    scope.defFor(name).resultType
  }

  def compile(
    moduleName: String,
    program: ast.Program,
    libraries: Seq[ast.Program] = Seq()): llvm.Module = scoped {

    val sources = libraries :+ program

    val statements = sources.flatMap(_.statements)

    val typeDefs = statements collect { case td: ast.TypeDef => td }

    val globals: Seq[ast.MemberDef] =
      statements collect { case d: ast.Def => d }

    val mainStatements = program.statements collect { case e: ast.Expression => e }

    // compilation ===

    defineTypes(typeDefs)

    scoped {
      setBlock(newBlock("entry"))
      mainStatements.foreach(compile)
      ret(llvm.Constant.Int(llvm.Type.Int(32), "0"))
    }

    defineInternal(
      llvm.Function(
        returnType = llvm.Type.Int(32),
        name = "main",
        basicBlocks = makeBasicBlocks()))

    module(moduleName)
  }

  def defineTypes(typeDefs: Seq[ast.TypeDef]): Unit = {
    for (typeDef <- typeDefs) {
      val aType = declare(typeDef.name)
      val companionType = declare(aType.companion)
      scope.put(ConstantDef(aType.name, companionType, unit))
    }

    for (typeDef <- typeDefs) {
      define(typeDef)
      println(s"defined: ${typeDef.name}")
    }
  }

  def define(typeDef: ast.TypeDef): Unit = scoped {
    val aType = typeFor(typeDef.name)

    scope.put(This(aType))

    val decls =
      for (defn <- typeDef.defs) yield {
        val d = declare(aType, defn)
        declare(aType, d)
        scope.put(d)
        d
      }

    for ((decl, defn) <- decls.zip(typeDef.defs)) {
      define(aType, defn, decl)
    }

    val defns = decls.map(d => d.signature -> d).toMap

    define(TypeDef(aType, defns))
    define(TypeDef(aType.companion, defns))
  }

  def declare(aType: Type, defn: ast.MemberDef): GlobalDef = defn match {
    case ast.Def(name, parameters, typeAnn, _) =>
      declare(aType, name, parameters, typeAnn)
    case ast.StaticDef(defn) =>
      declare(aType.companion, defn)
    case _: ast.StaticVarDef => ???
    case _: ast.VarDef       => ???
  }

  def declare(
    aType: Type,
    name: String,
    parameters: Option[Seq[ast.Parameter]],
    typeAnn: String): GlobalDef = {

    val paramTypes = parameters.map(_.map(p => typeFor(p.aType)))
    val llvmParams = llvm.Parameter(aType.toLlvm, "this") +: {
      paramTypes
        .getOrElse(Seq())
        .map(t => llvm.Parameter(t.toLlvm))
    }

    val paramClause = paramTypes.map(_.map(_.name)).getOrElse(Seq(""))
    val qualifiedName = QualifiedName(aType.name, name, paramClause: _*)

    val resultType = typeFor(typeAnn)
    val llvmResultType = resultType.toLlvm

    val reference = declare(llvmResultType, qualifiedName, llvmParams)

    GlobalDef(name, paramTypes, resultType, reference)
  }

  def define(
    aType: Type,
    defn: ast.MemberDef,
    decl: GlobalDef): Unit = {

    val GlobalDef(name, _, declaredType, reference) = decl

    defn match {
      case ast.Def(_, parameters, _, Some(body)) =>
        scoped {
          setBlock(newBlock("entry"))

          val ths = This(aType)
          val thisRef = alloca(aType.toLlvm)
          store(ths.reference, thisRef)
          scope.put(ths)

          val llvmParameters = llvm.Parameter(aType.toLlvm, "this") +: {
            for {
              params <- parameters.toList
              ast.Parameter(n, ta) <- params
            } yield {
              val aType = typeFor(ta)
              val llvmType = aType.toLlvm
              val pointerType = llvmType.pointer
              val localName = newLocalName()

              val reference = alloca(llvmType)
              store(llvm.LocalReference(llvmType, localName), reference)

              scope.put(LocalVarDef(n, aType, false, reference))

              llvm.Parameter(llvmType, localName)
            }
          }

          val (operand, actualType) = compile(body)

          if (actualType != declaredType)
            error(s"Declared result type $declaredType does not match actual type $actualType.")

          ret(operand)

          define(reference, llvmParameters, makeBasicBlocks())
        }

      case ast.Def(_, _, _, None) =>
      case ast.StaticDef(defn) =>
        define(aType.companion, defn, decl)
      case _: ast.VarDef => ???
      case _             => ???
    }
  }

  def compile(expression: ast.Expression): (llvm.Operand, Type) = {
    println(s"compiling: $expression")

    expression match {
      case ast.VarDef(name, typeAnn, expr, isMutable) =>
        val declaredType = typeFor(typeAnn)

        if (!scope.isTop) {
          val lt = declaredType.toLlvm
          val ref = alloca(lt)
          scope.put(LocalVarDef(name, declaredType, isMutable, ref))
        }

        val (operand, actualType) = compile(expr)

        if (actualType != declaredType)
          error(s"Declared type $declaredType does not match actual type $actualType.")

        val VarDef(_, _, _, ref) = scope.defFor(name)
        store(operand, ref)

        (unit, Unit)

      case ast.If(condition, thn, els) =>
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
        val resRef = alloca(lt)
        br(cond, thenBlock, elseBlock)

        setBlock(thenContinueBlock)
        store(e0, resRef)
        br(continueBlock)

        setBlock(elseContinueBlock)
        store(e1, resRef)
        br(continueBlock)

        setBlock(continueBlock)
        val result = load(lt, resRef)

        (result, t0)

      case ast.While(condition, body) =>
        val conditionBlock = newBlock("condition")
        val loopBlock = newBlock("loop")
        val continueBlock = newBlock("continue")

        br(conditionBlock)

        setBlock(conditionBlock)
        val (cond, condType) = compile(condition)

        if (condType != Bool)
          error(s"Condition evaluated to $condType, expected $Bool.")

        br(cond, loopBlock, continueBlock)

        setBlock(loopBlock)
        compile(body)
        br(conditionBlock)

        setBlock(continueBlock)

        (unit, Unit)

      case ast.Block(expressions) =>
        scoped {
          expressions.foldLeft((unit: llvm.Operand, Unit: Type)) {
            case (_, e) => compile(e)
          }
        }

      case ast.InfixExpression(e0, op, e1) =>
        val (v0, t0) = compile(e0)
        val (v1, t1) = compile(e1)

        defFor(t0, op, Some(Seq(t1))) match {
          case NativeFunDef(_, _, rt, body) => (body(Seq(v0, v1)), rt)
          case RefDef(_, _, resultType, ref) =>
            val result = call(resultType.toLlvm, ref, Seq(v0, v1))
            (result, resultType)
        }

      case ast.Id(name) =>
        val VarDef(_, aType, _, ref) = scope.defFor(name)
        (load(aType.toLlvm, ref), aType)

      case ast.Application(ast.Id(funName), arguments) =>
        val (args, argTypes) = arguments.map(compile).unzip

        val RefDef(_, _, resultType, ref) = scope.defFor(funName, Some(argTypes))

        val result = call(resultType.toLlvm, ref, args)

        (result, resultType)

      case ast.Application(ast.Selection(expr, name), arguments) =>
        val (e, t) = compile(expr)

        val (args, argTypes) = arguments.map(compile).unzip

        defFor(t, name, Some(argTypes)) match {
          case NativeFunDef(_, _, rt, body) => (body(e +: args), rt)
          case RefDef(_, _, resultType, ref) =>
            val result = call(resultType.toLlvm, ref, e +: args)
            (result, resultType)
        }

      case ast.Assignment(ast.Id(name), expr) =>
        val VarDef(_, declaredType, isMutable, ref) = scope.defFor(name)

        if (!isMutable)
          error(s"Cannot reassign into val $name.")

        val (o, actualType) = compile(expr)

        if (actualType != declaredType)
          error(s"Cannot assign type $actualType to $declaredType.")

        store(o, ref)

        (unit, Unit)

      case ast.BoolLiteral(value)   => (constantBool(value), Bool)
      case ast.CharLiteral(value)   => (constantChar(value), Char)
      case ast.StringLiteral(value) => ???
      case ast.IntLiteral(value)    => (constantInt(value), Int)
      case ast.FloatLiteral(value)  => (constantFloat(value), Float)
      case ast.Parenthesized(e)     => compile(e)
      case ast.Selection(where, what) =>
        val (e, t) = compile(where)

        defFor(t, what, None) match {
          case NativeFunDef(_, _, rt, body) => (body(Seq(e)), rt)
          case RefDef(_, _, resultType, ref) =>
            val result = call(resultType.toLlvm, ref, Seq(e))
            (result, resultType)
        }
      case e => println(e); ???
    }
  }

}
