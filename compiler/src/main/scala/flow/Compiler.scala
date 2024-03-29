package flow

import flow.mirror._
import flow.mirror.NativeTypes._
import flow.{ syntax => syn }
import flow.syntax.OperatorPrecedence
import flow.syntax.OperatorPrecedence
import java.lang.Boolean
import llvm.GlobalReference

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

    val libraryGlobals = libraries flatMap { _.statements collect { case d: syn.Def => d } }

    val programGlobals = program.statements collect { case d: syn.Def => d }

    val expressions = program.statements collect { case e: syn.Expression => e }

    // compilation ===

    val typeDecls = declareTypes(typeDefs)
    val globalDecls = declareGlobals(libraryGlobals)

    defineTypes(typeDecls)
    defineGlobals(globalDecls)

    scoped {
      defineGlobals(declareGlobals(programGlobals))

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
    }

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
    debug(s"Compiling $defn(${arguments.mkString(", ")}).")
    (defn.compile(arguments), defn.resultType)
  }

  def compile(expression: syn.Expression): CompiledExpression = {
    debug(s"Compiling: $expression.")

    expression match {
      case syn.VarDef(Seq(name), typeAnn, expr, isMutable) =>

        val e @ (_, actualType) = compile(expr)

        for (declaredType <- typeAnn.map(typeFor))
          if (actualType != declaredType)
            error(s"Declared type $declaredType does not match actual type $actualType.")

        compileVarDef(name, e, isMutable)
        (unit, Unit)

      case syn.VarDef(names, typeAnn, expr, isMutable) =>
        for (name <- names)
          compile(syn.VarDef(Seq(name), typeAnn, expr, isMutable))

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

      case syn.For(generators, body) =>
        if (generators.isEmpty) {
          compile(body)
        }
        else scoped {
          // TODO: dirty dirty hacks
          val range = "$range"
          val syn.Generator(name, expr, guard) = generators.head
          compile(syn.VarDef(Seq(range), None, expr, false))
          compile(syn.VarDef(Seq(name), None, syn.Selection(syn.Id(range), "start"), true))

          val whileCond = syn.InfixExpression(syn.Id(range), "contains", syn.Id(name))
          val increment = syn.InfixExpression(syn.Id(name), "+=", syn.Selection(syn.Id(range), "step"))
          val nextFor = syn.For(generators.tail, body)
          val nextForGuarded = guard map {
            guard =>
              syn.If(guard, nextFor, None)
          } getOrElse nextFor
          val whileBody = syn.Block(Seq(nextForGuarded, increment))

          compile(syn.While(whileCond, whileBody))
        }

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
        defFor(name) match {
          case Some(defn) => compile(defn)
          case None       => error(s"$name is not defined.")
        }

      case syn.Selection(expr, name) =>
        val (obj, aType) = compile(expr)
        defFor(aType, name) match {
          case Some(defn) => compile(defn, Seq(obj))
          case None       => error(s"Type $aType does not define $name.")
        }

      case syn.Application(id @ syn.Id(name), arguments) =>
        val compiledArguments = arguments.map(compile)
        val (args, argTypes) = compiledArguments.unzip
        defFor(name, Some(argTypes)) match {
          case Some(defn) => compile(defn, args)
          case None =>
            compileApplication(compile(id), "apply", Some(compiledArguments))
        }

      case syn.Application(syn.Selection(id: syn.Id, name), arguments) =>
        val compiledArguments = arguments.map(compile)
        compileApplication(compile(id), name, Some(compiledArguments), Some(id))

      case syn.Application(syn.Selection(expr, name), arguments) =>
        val compiledArguments = arguments.map(compile)
        compileApplication(compile(expr), name, Some(compiledArguments))

      case syn.Application(expr, arguments) =>
        val compiledArguments = arguments.map(compile)
        compileApplication(compile(expr), "apply", Some(compiledArguments))

      case syn.Assignment(syn.Id(name), argument) =>
        val compiledArgument = compile(argument)
        compileAssignment(name, compiledArgument)

      case syn.Assignment(syn.Selection(expr0, name), expr1) =>
        val (obj, aType) = compile(expr0)
        defFor(aType, name) match {
          case Some(defn) => assign(defn, compile(expr1))
          case None       => error(s"Type $aType does not define $name.")
        }

        (unit, Unit)

      case syn.Assignment(syn.Application(expr0, arguments), expr1) =>
        val obj = compile(expr0)
        val sub = compile(expr1)
        val compiledArguments = arguments.map(compile)
        compileApplication(obj, "update", Some(compiledArguments :+ sub))

      case syn.Assignment(_, _) =>
        error(s"$expression is not valid.")

      case syn.BoolLiteral(value)   => (constantBool(value), Bool)

      case syn.CharLiteral(value)   => (constantChar(value), Char)

      // TODO: StringLiteral
      case syn.StringLiteral(value) => (constantString(value), String)

      case syn.IntLiteral(value)    => (constantInt(value), Int)

      case syn.FloatLiteral(value)  => (constantFloat(value), Float)

      case syn.Parenthesized(e)     => compile(e)

      case e                        => println(e); ???
    }
  }

  def compileVarDef(name: String, compiledExpr: CompiledExpression, isMutable: Boolean) = {
    val e @ (value, aType) = compiledExpr
    val reference = alloca(aType.toLlvm)
    val defn = LocalVarDef(name, aType, isMutable, reference)

    scope.put(defn)
    assign(defn, e)

    defn
  }

  def compileApplication(
    compiledExpr: CompiledExpression,
    name: String,
    compiledArguments: Option[Seq[CompiledExpression]],
    assignTo: Option[syn.Id] = None): CompiledExpression = {

    val (obj, aType) = compiledExpr
    val unzipped = compiledArguments.map(_.unzip)
    val args = unzipped.map(_._1)
    val argTypes = unzipped.map(_._2)

    debug(s"Compiling Application ($aType, $name, $argTypes, $assignTo)")

    defFor(aType, name, argTypes) match {
      case Some(defn) =>
        compile(defn, obj +: args.getOrElse(Seq()))
      case None if name.endsWith("=") &&
        assignTo.isDefined =>
        val Some(syn.Id(lval)) = assignTo
        val compiledArgument = compileApplication(
          compiledExpr,
          name.substring(0, name.size - 1),
          compiledArguments)
        compileAssignment(lval, compiledArgument)
      case None =>
        val argClause = argTypes.map(_.mkString("(", ",", ")")).getOrElse("")
        error(s"Type $aType does not define $name$argClause.")
    }
  }

  def compileAssignment(name: String, compiledArgument: CompiledExpression) = {
    defFor(name) match {
      case Some(defn @ VarDef(_, declaredType, isMutable)) =>
        if (!isMutable)
          error(s"Cannot reassign into val $name.")

        val e @ (_, actualType) = compiledArgument

        if (actualType != declaredType)
          error(s"Cannot assign type $actualType to $declaredType.")

        assign(defn, e)

        (unit, Unit)
      case _ =>
        error(s"$name is not defined.")
    }
  }

  type TypeDeclaration = (Type, Seq[(Declaration, syn.MemberDef)])
  def declareTypes(typeDefs: Seq[syn.TypeDef]) = {
    for (typeDef <- typeDefs) {
      val aType = types_declare(typeDef.name)
      val companionType = types_declare(aType.companion)
      scope.put(ConstantDef(aType.name, companionType, unit))
    }

    for (typeDef <- typeDefs) yield {
      val aType = typeFor(typeDef.name)

      scoped {
        // TODO: `this`

        val decls =
          for (synDefn <- typeDef.defs) yield {
            (declare(aType, synDefn), synDefn)
          }

        (aType, decls)
      }
    }
  }

  def defineTypes(decls: Seq[TypeDeclaration]) = {
    for ((aType, decls) <- decls)
      for ((decl, defn) <- decls)
        define(aType, decl, defn)
  }

  def globalDef(
    name: String,
    parameterTypes: Option[Seq[Type]],
    resultType: Type,
    reference: llvm.GlobalReference) = {

    resultType match {
      case resultType: StructureType =>
        StructureReturningDef(name, parameterTypes, resultType, reference)
      case _ =>
        GlobalDef(name, parameterTypes, resultType, reference)
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

      // TODO: StructureReturningDef makes a mess
      val decl @ (_, reference, _, _) =
        declare(name, qualifiedName, Some(typedParametersWithThis), resultType)

      types_define(aType, globalDef(name, parameterTypes, resultType, reference))

      decl
    case syn.StaticDef(defn) =>
      declare(aType.companion, defn)
    case syn.VarDef(name, typeAnn, expr, isMutable) => ???
    case syn.StaticVarDef(defn)                     => ???
  }

  def define(aType: Type, decl: Declaration, defn: syn.MemberDef): Unit = defn match {
    case syn.Def(name, parameters, typeAnn, body) =>
      val (defn, ref, parameters, resultType) = decl
      define(defn, ref, parameters, resultType, body)
    case syn.StaticDef(defn) =>
      define(aType.companion, decl, defn)
    case syn.VarDef(name, typeAnn, expr, isMutable) => ???
    case syn.StaticVarDef(defn)                     => ???
  }

  type GlobalDeclaration = (Def, GlobalReference, Option[Seq[Parameter]], Type, Option[syn.Expression])
  def declareGlobals(globals: Seq[syn.Def]) = {
    for (synDefn <- globals) yield {
      val (defn, ref, parameters, resultType) = declare(synDefn)
      scope.put(defn)
      (defn, ref, parameters, resultType, synDefn.body)
    }
  }

  def defineGlobals(decls: Seq[GlobalDeclaration]) = {
    for ((defn, ref, parameters, resultType, body) <- decls)
      define(defn, ref, parameters, resultType, body)
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

  type Declaration = (Def, llvm.GlobalReference, Option[Seq[Parameter]], Type)

  // TODO: there's sill some copy&paste here
  /**
   * Declares definition for future use. Returns Declaration.
   */
  def declare(
    name: String,
    qualifiedName: QualifiedName,
    parameters: Option[Seq[Parameter]],
    resultType: Type): Declaration = {

    val parameterTypes = parameters.map(_.map(_.aType))
    val llvmParameterTypes = parameters.getOrElse(Seq()).map(_.aType.toLlvm)

    val (actualParameterTypes, returnType) = resultType match {
      case resultType: StructureType =>
        (resultType.toLlvm +: llvmParameterTypes, llvm.Type.Void)
      case _ =>
        (llvmParameterTypes, resultType.toLlvm)
    }

    val reference = global_declare(returnType, qualifiedName, actualParameterTypes)
    val defn = globalDef(name, parameterTypes, resultType, reference)

    (defn, reference, parameters, resultType)
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
    defn: Def,
    reference: llvm.GlobalReference,
    parameters: Option[Seq[Parameter]],
    declaredType: Type,
    body: Option[syn.Expression]): Unit = {

    body match {
      case Some(body) =>
        scoped {
          setBlock(newBlock("entry"))

          val parameterTypes = parameters.map(_.map(_.aType))
          val llvmParameters = parameters getOrElse Seq() map {
            case Parameter(name, aType) =>
              newParameter(name, aType)
          }

          val (actualParameters, returnType) = declaredType match {
            case resultType: StructureType =>
              // TODO: <sarcasm>this is really nice</sarcasm>
              val resultParameter = newParameter("", resultType)
              (resultParameter +: llvmParameters, llvm.Type.Void)
            case _ =>
              (llvmParameters, declaredType.toLlvm)
          }

          val (value, actualType) = compile(body)

          if (actualType != declaredType)
            error(s"Declared result type $declaredType does not match actual type $actualType.")

          defn match {
            case StructureReturningDef(_, _, resultType, _) =>
              defFor("") match {
                case Some(Ref(ref)) =>
                  val result = load(resultType.alias, value)
                  val ptr = load(resultType.toLlvm, ref)
                  store(result, ptr)
                  ret()
              }
            case _ =>
              ret(value)
          }

          global_define(reference, actualParameters, makeBasicBlocks())
        }
      case None =>
      // External definition.
    }
  }

}
