package flow

import flow.mirror._
import flow.mirror.NativeTypes._
import flow.{ syntax => syn }
import flow.syntax.OperatorPrecedence
import flow.syntax.OperatorPrecedence

trait Compiler
  extends GlobalCodegen
  with BlockCodegen
  with CompiledDefs
  with NativeTypes
  with Types
  with OperatorPrecedence
  with Scopes {

  def compile(
    moduleName: String,
    program: syn.Program,
    libraries: Seq[syn.Program]): llvm.Module = scoped {

    val sources = libraries :+ program

    val statements = sources.flatMap(_.statements)

    val typeDefs = statements collect { case td: syn.TypeDef => td }

    val globals = statements collect { case d: syn.Def => d }

    val expressions = program.statements collect { case e: syn.Expression => e }

    // compilation ===

    defineTypes(typeDefs)
    defineGlobals(globals)

    scoped {
      setBlock(newBlock("entry"))
      expressions.foreach(compile)
      ret(llvm.Constant.Int(llvm.Type.Int(32), "0"))
    }

    global_defineInternal(
      llvm.Function(
        returnType = llvm.Type.Int(32),
        name = "main",
        basicBlocks = makeBasicBlocks()))

    module(moduleName)
  }

  def assign(defn: Def, expr: CompiledExpression) = {
    val (value, _) = expr

    defn match {
      case LocalVarDef(_, declaredType, _, reference) =>
        store(value, reference)
    }
  }

  //   TODO: should this exist ?
  def compile(defn: Def, arguments: Seq[llvm.Operand] = Seq()) = {
    (defn.compile(arguments), defn.resultType)
  }

  def compile(expression: syn.Expression): CompiledExpression = {
    println(s"Compiling: $expression.")

    expression match {
      case syn.VarDef(name, typeAnn, expr, isMutable) =>
        val declaredType = typeFor(typeAnn)
        val reference = alloca(declaredType.toLlvm)
        val defn = LocalVarDef(name, declaredType, isMutable, reference)
        val e @ (_, actualType) = compile(expr)

        if (actualType != declaredType)
          error(s"Declared type $declaredType does not match actual type $actualType.")

        scope.put(defn)
        assign(defn, e)

        (unit, Unit)

      case syn.If(condition, thn, els) =>
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

      case syn.While(condition, body) =>
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

      case syn.Block(expressions) =>
        scoped {
          expressions.foldLeft((unit: llvm.Operand, Unit: Type)) {
            case (_, e) => compile(e)
          }
        }

      case syn.InfixExpression(e0, op, e1) =>
        val (obj, sub) = associativityOf(op) match {
          case Left  => (e0, e1)
          case Right => (e1, e0)
        }
        compile(syn.Application(syn.Selection(obj, op), Seq(sub)))

      case syn.Id(name) =>
        compile(defFor(name))

      case syn.Selection(expr, name) =>
        val (obj, aType) = compile(expr)
        compile(defFor(aType, name))

      case syn.Application(syn.Id(name), arguments) =>
        val (args, argTypes) = arguments.map(compile).unzip
        compile(defFor(name, Some(argTypes)), args)

      case syn.Application(syn.Selection(expr, name), arguments) =>
        val (obj, aType) = compile(expr)
        val (args, argTypes) = arguments.map(compile).unzip
        compile(defFor(aType, name, Some(argTypes)), obj +: args)

      case syn.Assignment(syn.Id(name), expr) =>
        val defn @ VarDef(_, declaredType, isMutable) = defFor(name)

        if (!isMutable)
          error(s"Cannot reassign into val $name.")

        val e @ (_, actualType) = compile(expr)

        if (actualType != declaredType)
          error(s"Cannot assign type $actualType to $declaredType.")

        assign(defn, e)

        (unit, Unit)

      case syn.BoolLiteral(value)   => (constantBool(value), Bool)

      case syn.CharLiteral(value)   => (constantChar(value), Char)

      case syn.StringLiteral(value) => ???

      case syn.IntLiteral(value)    => (constantInt(value), Int)

      case syn.FloatLiteral(value)  => (constantFloat(value), Float)

      case syn.Parenthesized(e)     => compile(e)

      case e                        => println(e); ???
    }
  }

  def defineTypes(typeDefs: Seq[syn.TypeDef]): Unit = {
    for (typeDef <- typeDefs) {
      val aType = types_declare(typeDef.name)
      types_declare(aType.companion)
    }

    for (typeDef <- typeDefs) {
      val aType = typeFor(typeDef.name)

      scoped {
        // TODO: `this`

        val decls =
          for (synDefn <- typeDef.defs) yield {
            declare(aType, synDefn)
          }

        for ((synDefn, decl) <- typeDef.defs.zip(decls)) {
          define(aType, synDefn, decl)
        }
      }
    }

  }

  def declare(aType: Type, defn: syn.MemberDef): Declaration = defn match {
    case syn.Def(name, parameters, typeAnn, body) =>
      val typedParameters = typedParametersFrom(parameters)
      val parameterTypes = typedParameters.map(_.map(_.aType))
      val typedParametersWithThis =
        This(aType) +: typedParameters.getOrElse(Seq())
      val parameterTypeNames =
        typedParametersWithThis.map(_.aType.name) ++ (if (parameters.isEmpty) Seq("") else Seq())
      val qualifiedName =
        QualifiedName(aType.name +: name +: parameterTypeNames: _*)
      val resultType = typeFor(typeAnn)

      val decl @ (GlobalDef(_, _, _, reference), _, _) =
        declare(name, qualifiedName, Some(typedParametersWithThis), resultType)

      types_define(aType, GlobalDef(name, parameterTypes, resultType, reference))

      decl
    case syn.StaticDef(defn) =>
      declare(aType.companion, defn)
    case syn.VarDef(name, typeAnn, expr, isMutable) => ???
    case syn.StaticVarDef(defn)                     => ???
  }

  def define(aType: Type, defn: syn.MemberDef, decl: Declaration): Unit = defn match {
    case syn.Def(name, parameters, typeAnn, body) =>
      val (defn, parameters, resultType) = decl
      define(defn, parameters, resultType, body)
    case syn.StaticDef(defn) =>
      define(aType.companion, defn, decl)
    case syn.VarDef(name, typeAnn, expr, isMutable) => ???
    case syn.StaticVarDef(defn)                     => ???
  }

  def defineGlobals(globals: Seq[syn.Def]) = {
    val decls =
      for (synDefn <- globals) yield {
        val (defn, parameters, resultType) = declare(synDefn)
        scope.put(defn)
        (defn, parameters, resultType, synDefn.body)
      }

    for ((defn, parameters, resultType, body) <- decls)
      define(defn, parameters, resultType, body)
  }

  def declare(defn: syn.Def): Declaration = defn match {
    case syn.Def(name, parameters, typeAnn, body) =>
      val typedParameters = typedParametersFrom(parameters)
      val parameterTypeNames = typedParameters.map(_.map(_.aType.name)).getOrElse(Seq(""))
      val qualifiedName = QualifiedName(name +: parameterTypeNames: _*)
      val resultType = typeFor(typeAnn)

      declare(name, qualifiedName, typedParameters, resultType)
  }

  def typedParameterFrom(parameter: syn.Parameter) = parameter match {
    case syn.Parameter(name, typeAnn) => Parameter(name, typeFor(typeAnn))
  }

  def typedParametersFrom(parameters: Option[Seq[syn.Parameter]]) =
    parameters.map(_.map(typedParameterFrom))

  type Declaration = (GlobalDef, Option[Seq[Parameter]], Type)

  /**
   * Declares definition for future use. Returns GlobalDef.
   */
  def declare(
    name: String,
    qualifiedName: QualifiedName,
    parameters: Option[Seq[Parameter]],
    resultType: Type): Declaration = {

    val parameterTypes = parameters.map(_.map(_.aType))

    val llvmParameters = parameters.getOrElse(Seq()).map(p => llvm.Parameter(p.aType.toLlvm))
    val returnType = resultType.toLlvm

    val reference = global_declare(returnType, qualifiedName, llvmParameters)
    val defn = GlobalDef(name, parameterTypes, resultType, reference)

    (defn, parameters, resultType)
  }

  def newParameter(name: String, aType: Type) = {
    val llvmType = aType.toLlvm
    val llvmName = newLocalName()
    val ref = alloca(llvmType)
    store(llvm.LocalReference(llvmType, llvmName), ref)
    scope.put(LocalVarDef(name, aType, false, ref))
    llvm.Parameter(llvmType, llvmName)
  }

  /**
   * Defines previously declared definition.
   */
  def define(
    defn: GlobalDef,
    parameters: Option[Seq[Parameter]],
    declaredType: Type,
    body: Option[syn.Expression]): Unit = {

    val reference = defn.reference

    body match {
      case Some(body) =>
        scoped {
          setBlock(newBlock("entry"))
          val llvmParameters = parameters map {
            _ map {
              case Parameter(name, aType) =>
                newParameter(name, aType)
            }
          } getOrElse Seq()

          val (value, actualType) = compile(body)

          if (actualType != declaredType)
            error(s"Declared result type $declaredType does not match actual type $actualType.")

          ret(value)

          global_define(reference, llvmParameters, makeBasicBlocks())
        }
      case None =>
      // External definition.
    }
  }

  // DO NOT CROSS THIS LINE ====================================================

  //  def define(typeDef: syn.TypeDef): Unit = scoped {
  //    val aType = typeFor(typeDef.name)
  //
  //    scopes.put(This(aType))
  //
  //    val decls =
  //      for (defn <- typeDef.defs) yield {
  //        val d = declare(aType, defn)
  //        declare(aType, d)
  //        scopes.put(d)
  //        d
  //      }
  //
  //    for ((decl, defn) <- decls.zip(typeDef.defs)) {
  //      define(aType, defn, decl)
  //    }
  //
  //    val defns = decls.map(d => d.signature -> d).toMap
  //
  //    define(TypeDef(aType, defns))
  //    define(TypeDef(aType.companion, defns))
  //  }
  //
  //  def declare(aType: Type, defn: syn.MemberDef): GlobalDef = defn match {
  //    case syn.Def(name, parameters, typeAnn, _) =>
  //      declare(aType, name, parameters, typeAnn)
  //    case syn.StaticDef(defn) =>
  //      declare(aType.companion, defn)
  //    case _: syn.StaticVarDef => ???
  //    case _: syn.VarDef       => ???
  //  }
  //
  //  def declare(
  //    aType: Type,
  //    name: String,
  //    parameters: Option[Seq[syn.Parameter]],
  //    typeAnn: String): GlobalDef = {
  //
  //    val paramTypes = parameters.map(_.map(p => typeFor(p.aType)))
  //    val llvmParams = llvm.Parameter(aType.toLlvm, "this") +: {
  //      paramTypes
  //        .getOrElse(Seq())
  //        .map(t => llvm.Parameter(t.toLlvm))
  //    }
  //
  //    val paramClause = paramTypes.map(_.map(_.name)).getOrElse(Seq(""))
  //    val qualifiedName = QualifiedName(aType.name, name, paramClause: _*)
  //
  //    val resultType = typeFor(typeAnn)
  //    val llvmResultType = resultType.toLlvm
  //
  //    val reference = declare(llvmResultType, qualifiedName, llvmParams)
  //
  //    GlobalDef(name, paramTypes, resultType, reference)
  //  }
  //
  //  def define(
  //    aType: Type,
  //    defn: syn.MemberDef,
  //    decl: GlobalDef): Unit = {
  //
  //    val GlobalDef(name, _, declaredType, reference) = decl
  //
  //    defn match {
  //      case syn.Def(_, parameters, _, Some(body)) =>
  //        scoped {
  //          setBlock(newBlock("entry"))
  //
  //          val ths = This(aType)
  //          val thisRef = alloca(aType.toLlvm)
  //          store(ths.reference, thisRef)
  //          scopes.put(ths)
  //
  //          val llvmParameters = llvm.Parameter(aType.toLlvm, "this") +: {
  //            for {
  //              params <- parameters.toList
  //              syn.Parameter(n, ta) <- params
  //            } yield {
  //              val aType = typeFor(ta)
  //              val llvmType = aType.toLlvm
  //              val pointerType = llvmType.pointer
  //              val localName = newLocalName()
  //
  //              val reference = alloca(llvmType)
  //              store(llvm.LocalReference(llvmType, localName), reference)
  //
  //              scopes.put(LocalVarDef(n, aType, false, reference))
  //
  //              llvm.Parameter(llvmType, localName)
  //            }
  //          }
  //
  //          val (operand, actualType) = compile(body)
  //
  //          if (actualType != declaredType)
  //            error(s"Declared result type $declaredType does not match actual type $actualType.")
  //
  //          ret(operand)
  //
  //          define(reference, llvmParameters, makeBasicBlocks())
  //        }
  //
  //      case syn.Def(_, _, _, None) =>
  //      case syn.StaticDef(defn) =>
  //        define(aType.companion, defn, decl)
  //      case _: syn.VarDef => ???
  //      case _             => ???
  //    }
  //  }

}
