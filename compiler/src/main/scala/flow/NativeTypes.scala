package flow

import llvm.{ IntegerPredicate => IP, FloatingPointPredicate => FPP, _ }

object NativeTypes {

  val Bool = NativeType("Bool", llvm.Type.Int(1))

  val Char = NativeType("Char", llvm.Type.Int(8))

  val Int = NativeType("Int", llvm.Type.Int(64))

  val Float = NativeType("Float", llvm.Type.Double)

  val Unit = NativeType("Unit", llvm.Type.Int(1))

  def get(name: String): Option[Type] = name match {
    case "Bool"  => Some(Bool)
    case "Char"  => Some(Char)
    case "Int"   => Some(Int)
    case "Float" => Some(Float)
    case "Unit"  => Some(Unit)
    case _       => None
  }

}

trait NativeTypes extends NativeDefs {
  self: GlobalCodegen with BlockCodegen =>
  import NativeTypes._

  def nativeDefFor(
    aType: NativeType[_],
    name: String,
    parameterTypes: Option[Seq[Type]]): Option[Def] =

    nativeDefFor(aType, Signature(name, parameterTypes))

  def nativeDefFor(aType: NativeType[_], signature: Signature): Option[Def] = {
    val typeDef = nativeTypeDefOf(aType)
    typeDef.defs.get(signature)
  }

  lazy val boolPrint =
    defineInternal(
      Function(
        returnType = Unit.toLlvm,
        name = "Bool_print",
        parameters = Seq(Parameter(Bool.toLlvm))))

  lazy val intPrint =
    defineInternal(
      Function(
        returnType = Unit.toLlvm,
        name = "Int_print",
        parameters = Seq(Parameter(Int.toLlvm))))

  lazy val floatPrint =
    defineInternal(
      Function(
        returnType = Unit.toLlvm,
        name = "Float_print",
        parameters = Seq(Parameter(Float.toLlvm))))

  lazy val unitPrint =
    defineInternal(
      Function(
        returnType = Unit.toLlvm,
        name = "Unit_print",
        parameters = Seq()))

  lazy val nativeTypeDefOf = Seq(
    TypeDef(
      Bool,
      NativeFunDef("print", Seq(), Unit, {
        case Seq(a) =>
          instruction(Call(boolPrint, Seq((a, Seq()))))
          unit
      })),

    TypeDef(
      Int,
      NativeFunDef("+", Seq(Int), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, Add(a, b))
      }),
      NativeFunDef("-", Seq(Int), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, Sub(a, b))
      }),
      NativeFunDef("*", Seq(Int), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, Mul(a, b))
      }),
      NativeFunDef("/", Seq(Int), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, SDiv(a, b))
      }),
      NativeFunDef("%", Seq(Int), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, SRem(a, b))
      }),
      NativeFunDef("==", Seq(Int), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.EQ, a, b))
      }),
      NativeFunDef("!=", Seq(Int), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.NE, a, b))
      }),
      NativeFunDef("<", Seq(Int), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SLT, a, b))
      }),
      NativeFunDef("<=", Seq(Int), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SLE, a, b))
      }),
      NativeFunDef(">", Seq(Int), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SGT, a, b))
      }),
      NativeFunDef(">=", Seq(Int), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SGE, a, b))
      }),
      NativeFunDef("toFloat", Seq(), Float, {
        case Seq(a) => instruction(Float.toLlvm, SIToFP(a, Float.toLlvm))
      }),
      NativeFunDef("print", Seq(), Unit, {
        case Seq(a) =>
          instruction(Call(intPrint, Seq((a, Seq()))))
          unit
      })),

    TypeDef(
      Float,
      NativeFunDef("+", Seq(Float), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FAdd(a, b))
      }),
      NativeFunDef("-", Seq(Float), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FSub(a, b))
      }),
      NativeFunDef("*", Seq(Float), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FMul(a, b))
      }),
      NativeFunDef("/", Seq(Float), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FDiv(a, b))
      }),
      NativeFunDef("==", Seq(Float), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OEQ, a, b))
      }),
      NativeFunDef("!=", Seq(Float), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.ONE, a, b))
      }),
      NativeFunDef("<", Seq(Float), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OLT, a, b))
      }),
      NativeFunDef("<=", Seq(Float), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OLE, a, b))
      }),
      NativeFunDef(">", Seq(Float), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OGT, a, b))
      }),
      NativeFunDef(">=", Seq(Float), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OGE, a, b))
      }),
      NativeFunDef("floor", Seq(), Int, {
        case Seq(a) => instruction(Int.toLlvm, FPToSI(a, Int.toLlvm))
      }),
      NativeFunDef("print", Seq(), Unit, {
        case Seq(a) =>
          instruction(Call(floatPrint, Seq((a, Seq()))))
          unit
      })),

    TypeDef(
      Unit,
      NativeFunDef("print", Seq(), Unit, {
        case Seq(u) =>
          instruction(Call(unitPrint, Seq()))
          unit
      })))
    .map(td => td.aType -> td)
    .toMap

  def constantBool(value: Boolean) =
    if (value) llvm.Constant.True
    else llvm.Constant.False

  def constantChar(value: Char) =
    llvm.Constant.Int(Char.toLlvm, value.toInt.toString)

  def constantInt(value: BigInt) =
    llvm.Constant.Int(Int.toLlvm, value.toString)

  def constantFloat(value: String) =
    llvm.Constant.Float(Float.toLlvm, value)

  def unit =
    constantBool(true)

}
