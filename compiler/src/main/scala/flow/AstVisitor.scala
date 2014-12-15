package flow

import scala.collection.JavaConversions._

import FlowParser.{Id => _, While => _, BoolLiteral => _, CharLiteral => _, StringLiteral => _, IntLiteral => _, FloatLiteral => _, _}
import ast._

class AstVisitor extends FlowBaseVisitor[Ast] {
  override def visitProg(context: ProgContext) =
    Program(context.stat.map(s => visit(s).asInstanceOf[Statement]))
  
  override def visitVarVarDef(context: VarVarDefContext) = {
    val defRest = context.defRest
    val declIds = defRest.defPat.declIds
    val ids = declIds.Id.map(_.getText())
    val typeAnn = Option(declIds.typeAnn).map(_.Id.getText())
    val expr = visit(defRest.expr).asInstanceOf[Expression]
    
    VarDef(ids, typeAnn, expr, false)
  }
  
  override def visitValVarDef(context: ValVarDefContext) = {
    val defRest = context.defRest
    val declIds = defRest.defPat.declIds
    val ids = declIds.Id.map(_.getText())
    val typeAnn = Option(declIds.typeAnn).map(_.Id.getText())
    val expr = visit(defRest.expr).asInstanceOf[Expression]
    
    VarDef(ids, typeAnn, expr, true)
  }
  
  override def visitBlock(context: BlockContext) =
    Block(context.expr.map(s => visit(s).asInstanceOf[Expression]))
  
  override def visitBranch(context: BranchContext) = {
    val cond = visit(context.cond).asInstanceOf[Expression]
    val thenExpr = visit(context.thenE).asInstanceOf[Expression]
    val elseExpr = Option(context.elseE).map(e => visit(e).asInstanceOf[Expression])
    
    Branch(cond, thenExpr, elseExpr)
  }
  
  override def visitWhile(context: WhileContext) = {
    val cond = visit(context.cond).asInstanceOf[Expression]
    val body = visit(context.body).asInstanceOf[Expression]
    
    While(cond, body)
  }
  
  override def visitAssignment(context: AssignmentContext) = {
    val id = context.Id.getText()
    val expr = visit(context.expr).asInstanceOf[Expression]
    
    Assignment(id, expr)
  }
  
  override def visitCall(context: CallContext) = {
    val id = context.Id.getText()
    val args =
      if (context.args == null) Seq.empty[Expression]
      else context.args.expr.map(e => visit(e).asInstanceOf[Expression])
      
    Call(id, args)
  }
  
  override def visitId(context: IdContext) =
    Id(context.Id.getText())
  
  override def visitExternalDef(context: ExternalDefContext) = {
    val name = context.defSig.Id.getText()
    val paramClause = context.defSig.paramClause
    val params = paramClause.params.param.map(p => visit(p).asInstanceOf[Parameter])
    val typeAnn = context.typeAnn.Id.getText
    
    ExternalDef(name, params, false, typeAnn)
  }
  
  override def visitInternalDef(context: InternalDefContext) = {
    val name = context.defSig.Id.getText()
    val paramClause = Option(context.defSig.paramClause)
    val params = paramClause map { clause =>
      if (clause.params == null) Seq.empty[Parameter]
      else clause.params.param.map(p => visit(p).asInstanceOf[Parameter])
    }
    val typeAnn = Option(context.typeAnn).map(t => t.Id.getText)
    val body = visit(context.expr).asInstanceOf[Expression]
    
    InternalDef(name, params, false, typeAnn, body)
  }
  
  override def visitParam(context: ParamContext) =
    Parameter(context.Id.getText, context.typeAnn.Id.getText)
  
  override def visitBool(context: BoolContext) =
    BoolLiteral(context.BoolLiteral.getText())
  
  override def visitChar(context: CharContext) =
    CharLiteral(context.CharLiteral.getText())
  
  override def visitString(context: StringContext) =
    StringLiteral(context.StringLiteral.getText())
  
  override def visitInt(context: IntContext) =
    IntLiteral(context.IntLiteral.getText())
  
  override def visitFloat(context: FloatContext) =
    FloatLiteral(context.FloatLiteral.getText())
}
