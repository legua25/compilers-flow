package flow.mirror

import NativeTypes._
import flow.BlockCodegen
import flow.GlobalCodegen

import llvm.{ IntegerPredicate => IP, FloatingPointPredicate => FPP, _ }

object NativeTypes {

  val Bool = NativeType("Bool", llvm.Type.Int(1))

  val Char = NativeType("Char", llvm.Type.Int(8))

  val String = NativeType("String", llvm.Type.Int(8))

  val Int = NativeType("Int", llvm.Type.Int(64))

  val Float = NativeType("Float", llvm.Type.Double)

  val Unit = NativeType("Unit", llvm.Type.Int(1))

  val IntArray = NativeType("IntArray", llvm.Type.Int(8))

  def get(name: String): Option[Type] = name match {
    case "Bool"     => Some(Bool)
    case "Char"     => Some(Char)
    case "Int"      => Some(Int)
    case "String"   => Some(String)
    case "Float"    => Some(Float)
    case "Unit"     => Some(Unit)
    case "IntArray" => Some(IntArray)
    case _          => None
  }

}

trait NativeTypes {
  self: GlobalCodegen with BlockCodegen with CompiledDefs =>
  import NativeTypes._

  def nativeDefFor(aType: Type, signature: Signature): Option[Def] = {
    for {
      typeDef <- nativeTypeDefs.get(aType)
      defn <- typeDef.defs.get(signature)
    } yield defn
  }

  val nativeTypeDefs = Seq(
    TypeDef(
      Bool,
      NativeFunDef("==", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.EQ, a, b))
      }),
      NativeFunDef("!=", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.NE, a, b))
      }),
      NativeFunDef("<", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.ULT, a, b))
      }),
      NativeFunDef("<=", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.ULE, a, b))
      }),
      NativeFunDef(">", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.UGT, a, b))
      }),
      NativeFunDef(">=", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.UGE, a, b))
      }),
      NativeFunDef("not", None, Bool, {
        case Seq(a) => instruction(Bool.toLlvm, Xor(a, Constant.True))
      }),
      NativeFunDef("&&", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, And(a, b))
      }),
      NativeFunDef("||", Some(Seq(Bool)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, Or(a, b))
      })),

    TypeDef(
      Char,
      NativeFunDef("==", Some(Seq(Char)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.EQ, a, b))
      }),
      NativeFunDef("!=", Some(Seq(Char)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.NE, a, b))
      }),
      NativeFunDef("<", Some(Seq(Char)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.ULT, a, b))
      }),
      NativeFunDef("<=", Some(Seq(Char)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.ULE, a, b))
      }),
      NativeFunDef(">", Some(Seq(Char)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.UGT, a, b))
      }),
      NativeFunDef(">=", Some(Seq(Char)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.UGE, a, b))
      })),

    TypeDef(
      Int,
      NativeFunDef("==", Some(Seq(Int)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.EQ, a, b))
      }),
      NativeFunDef("!=", Some(Seq(Int)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.NE, a, b))
      }),
      NativeFunDef("<", Some(Seq(Int)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SLT, a, b))
      }),
      NativeFunDef("<=", Some(Seq(Int)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SLE, a, b))
      }),
      NativeFunDef(">", Some(Seq(Int)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SGT, a, b))
      }),
      NativeFunDef(">=", Some(Seq(Int)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, ICmp(IP.SGE, a, b))
      }),
      NativeFunDef("+", Some(Seq(Int)), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, Add(a, b))
      }),
      NativeFunDef("-", Some(Seq(Int)), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, Sub(a, b))
      }),
      NativeFunDef("*", Some(Seq(Int)), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, Mul(a, b))
      }),
      NativeFunDef("/", Some(Seq(Int)), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, SDiv(a, b))
      }),
      NativeFunDef("%", Some(Seq(Int)), Int, {
        case Seq(a, b) => instruction(Int.toLlvm, SRem(a, b))
      }),
      NativeFunDef("toFloat", None, Float, {
        case Seq(a) => instruction(Float.toLlvm, SIToFP(a, Float.toLlvm))
      })),

    TypeDef(
      Float,
      NativeFunDef("==", Some(Seq(Float)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OEQ, a, b))
      }),
      NativeFunDef("!=", Some(Seq(Float)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.ONE, a, b))
      }),
      NativeFunDef("<", Some(Seq(Float)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OLT, a, b))
      }),
      NativeFunDef("<=", Some(Seq(Float)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OLE, a, b))
      }),
      NativeFunDef(">", Some(Seq(Float)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OGT, a, b))
      }),
      NativeFunDef(">=", Some(Seq(Float)), Bool, {
        case Seq(a, b) => instruction(Bool.toLlvm, FCmp(FPP.OGE, a, b))
      }),
      NativeFunDef("+", Some(Seq(Float)), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FAdd(a, b))
      }),
      NativeFunDef("-", Some(Seq(Float)), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FSub(a, b))
      }),
      NativeFunDef("*", Some(Seq(Float)), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FMul(a, b))
      }),
      NativeFunDef("/", Some(Seq(Float)), Float, {
        case Seq(a, b) => instruction(Float.toLlvm, FDiv(a, b))
      }),
      NativeFunDef("toInt", None, Int, {
        case Seq(a) => instruction(Int.toLlvm, FPToSI(a, Int.toLlvm))
      })),

    TypeDef(Unit))
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
