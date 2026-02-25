import java.util.HashMap;
import java.util.Map;

import javax.imageio.IIOException;

import org.stringtemplate.v4.*;

@SuppressWarnings("CheckReturnValue")
public class IMLCompiler extends IMLParserBaseVisitor<ST> {

   private STGroup templates = new STGroupFile("IML.stg");
   private HashMap<String, String> varNames = new HashMap<>();
   private int varNumb = 0;
   private SymbolTable symbolTable = new SymbolTable();

   private String newVarName(){
      
      String name = "v" + varNumb++;
      varNames.put("image "+varNumb, name);

      return name;

   }

   private String newVarName(String varOldName){
      
      String name = "v" + varNumb++;
      varNames.put(varOldName, name);

      return name;
   }

   private String getVarName(String varOldName){
      if (varNames.containsKey(varOldName)){
         return varNames.get(varOldName);
      }

      return newVarName(varOldName);
   }

   private Type changeTypeList(ListType list, Type newType){
      if (list.getElementType() instanceof ListType){
         return new ListType(changeTypeList((ListType) list.getElementType(), newType));
      }
      return new ListType(newType);
   } 

   @Override public ST visitProgram(IMLParser.ProgramContext ctx) {
      ST res = templates.getInstanceOf("main");

      for (IMLParser.StatContext stat : ctx.stat()){
         ST statRes = visit(stat);
         res.add("stat", statRes.render()); // render the return value!
      }

      return res;
   }

   @Override public ST visitInstantiationStat(IMLParser.InstantiationStatContext ctx) {
      return visit(ctx.instantiation());
   }

   @Override public ST visitAssignmentStat(IMLParser.AssignmentStatContext ctx) {
      return visit(ctx.assignment());
   }

   @Override public ST visitIfStat(IMLParser.IfStatContext ctx) {
      return visit(ctx.ifStatement());
   }

   @Override public ST visitUntilStat(IMLParser.UntilStatContext ctx) {
      return visit(ctx.untilLoop());
   }

   @Override public ST visitForStat(IMLParser.ForStatContext ctx) {
      return visit(ctx.forLoop());

   }

   @Override public ST visitCommandStat(IMLParser.CommandStatContext ctx) {
      return visit(ctx.command());
   }

   @Override public ST visitSaveStat(IMLParser.SaveStatContext ctx) {
      return visit(ctx.save());
   }

   @Override public ST visitListOpStat(IMLParser.ListOpStatContext ctx) {
      return visit(ctx.listOperation());
   }

   @Override public ST visitInstantiation(IMLParser.InstantiationContext ctx) {
      ST res = visit(ctx.assignment());
      symbolTable.declare(ctx.assignment().varName, ctx.typeID().res);
      return res;
   }

   @Override public ST visitAssignment(IMLParser.AssignmentContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());
      String varName = getVarName(ctx.ID().getText());
      ctx.varName = varName;
      res.add("var", varName);
      res.add("value", ctx.expression().varName);
      return res;
   }

   @Override public ST visitPrintCommand(IMLParser.PrintCommandContext ctx) {
      
      ST res = templates.getInstanceOf("output");
      res.add("stat", visit(ctx.expression()).render());
      String outputVar = ctx.expression().varName;
      if (ctx.expression().eType instanceof ImageType){
         ST assignSt = templates.getInstanceOf("assign");
         outputVar = newVarName();
         assignSt.add("var", outputVar);
         assignSt.add("value", ctx.expression().varName+".shape");
         res.add("stat", assignSt.render());
      }
      res.add("out", outputVar);
      return res;
   }

   @Override public ST visitDrawCommand(IMLParser.DrawCommandContext ctx) {
      ST res = templates.getInstanceOf("draw");
      res.add("stat", visit(ctx.expression()).render());
      res.add("img", ctx.expression().varName);

      return res;
   }

   @Override public ST visitSave(IMLParser.SaveContext ctx) {

      ST expr1ST = visit(ctx.e1);
      ST expr2ST = visit(ctx.e2);
      ST res = null;
      
      if ( ctx.e1.eType instanceof ListType){
         ListType list = (ListType) ctx.e1.eType;
         Type subType = list.getBasicType();
         if (subType instanceof ImageType){
            res = templates.getInstanceOf("saveGIF");
            res.add("img", ctx.e1.varName);
         }
         else {
               res = templates.getInstanceOf("store");
               res.add("img", "(" + ctx.e1.varName+"*255).astype(np.uint8)");
         }
      }else {
         res = templates.getInstanceOf("store");
         res.add("img", ctx.e1.varName);
      }
      
      res.add("stat", expr1ST.render());
      res.add("stat", expr2ST.render());
      res.add("path", ctx.e2.varName);

      return res;
   }

   @Override
   public ST visitIfStatement(IMLParser.IfStatementContext ctx) {
      ST res = templates.getInstanceOf("ifStatement");

      ST condExpr = visit(ctx.expression());
      res.add("preStat", condExpr.render());
      res.add("cond", ctx.expression().varName);

      res.add("trueStat", visit(ctx.thenBlock));

      if (ctx.elseBlock != null) {
         res.add("falseStat", visit(ctx.elseBlock));
      }

      return res;
   }

   
   @Override
   public ST visitStatBlock(IMLParser.StatBlockContext ctx) {
      ST res = templates.getInstanceOf("stats");
      for (IMLParser.StatContext stat : ctx.stat()) {
         ST statRes = visit(stat);
         res.add("stat", statRes.render());
      }
      return res;
   }

	

   @Override public ST visitUntilLoop(IMLParser.UntilLoopContext ctx) {
      ST res = templates.getInstanceOf("untilLoop");

      ST condExpr = visit(ctx.expression());
      res.add("preStat", condExpr.render());
      res.add("cond", ctx.expression().varName);

      res.add("loopStat", visit(ctx.statBlock()).render());

      return res;
   }

   @Override public ST visitForLoop(IMLParser.ForLoopContext ctx) {
      ST res = templates.getInstanceOf("forLoop");

      res.add("preStat", visit(ctx.expression()).render());
      String varName = getVarName(ctx.ID().getText());

      ListType listType = (ListType) ctx.expression().eType;
      symbolTable.declare(varName, listType.getElementType());

      res.add("var", varName);
      res.add("list", ctx.expression().varName);

      res.add("loopStat", visit(ctx.statBlock()).render());

      return res;
   }

   @Override public ST visitListOperation(IMLParser.ListOperationContext ctx) {
      ST res = templates.getInstanceOf("list_"+ ctx.op.getText());
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      res.add("list", ctx.e1.varName);
      res.add("elem", ctx.e2.varName);
      return res;
   }

   @Override public ST visitExprReverse(IMLParser.ExprReverseContext ctx) {

      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());
      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("boolean");
      res.add("var", ctx.varName);
      res.add("value", "not " + ctx.expression().varName);
      return res;

   }

   @Override public ST visitExprRun(IMLParser.ExprRunContext ctx) {
      ST res = templates.getInstanceOf("assign");
      
      res.add("stat", visit(ctx.expression()).render());

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("image");

      ST runT = templates.getInstanceOf("exprRun");
      runT.add("cmd", ctx.expression().varName);
      res.add("value", runT.render());

      return res;
   }

   @Override public ST visitExprBoolean(IMLParser.ExprBooleanContext ctx) {
    ST res = templates.getInstanceOf("assign");
    ctx.varName = newVarName();
    res.add("var", ctx.varName);
    ctx.eType = symbolTable.getTypeByName("boolean");
    res.add("value", ctx.BOOLEAN().getText().toLowerCase());
    return res;
   }

   @Override public ST visitExprHats(IMLParser.ExprHatsContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("image");

      ST hatOp = templates.getInstanceOf("hatOp");
      String flag = ctx.op.getText().equals("TOP") ? "cv2.MORPH_TOPHAT" : "cv2.MORPH_BLACKHAT";
      hatOp.add("op", flag);
      if (ctx.e1.eType instanceof ListType){
         hatOp.add("img", "np.array("+ctx.e1.varName+",np.uint8)");
      } else{
         hatOp.add("img", ctx.e1.varName);
      }
      if (ctx.e2.eType instanceof ListType){
         hatOp.add("kernel", "np.array("+ctx.e2.varName+",np.uint8)");
      } else{
         hatOp.add("kernel", ctx.e2.varName);
      }
      res.add("value", hatOp.render());
      return res;
   }

   @Override public ST visitExprAddSub(IMLParser.ExprAddSubContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      ctx.eType = ctx.e1.eType;
      res.add("var", ctx.varName);
      res.add("value", ctx.e1.varName + " " + ctx.op.getText() + " " + ctx.e2.varName);
      return res;

   }
   

   @Override public ST visitExprPxEqNeq(IMLParser.ExprPxEqNeqContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      res.add("var", ctx.varName);

      String operand1 = ctx.e1.varName;
      String operand2 = ctx.e2.varName;

      if (ctx.e1.eType instanceof ListType){
         ctx.eType = changeTypeList((ListType) ctx.e1.eType, symbolTable.getTypeByName("boolean"));
      }
      else if (ctx.e2.eType instanceof ListType){
         ctx.eType = changeTypeList((ListType) ctx.e2.eType, symbolTable.getTypeByName("boolean"));
      }
      else{
         // Them it's an image so the type is gonna be an list of list of booleans
         ctx.eType = new ListType(new ListType(symbolTable.getTypeByName("boolean")));
         if (ctx.e1.eType instanceof NumberType){
            operand1 += "*255";
         }else if (ctx.e2.eType instanceof NumberType){
            operand2 += "*255";
         }
      }

      // retira o "."
      String rawOp = ctx.op.getText();
      String pyOp = rawOp.substring(1);
      res.add("value", operand1 + " " + pyOp + " " + operand2);
      return res;
   }

   @Override public ST visitExprColumns(IMLParser.ExprColumnsContext ctx) {

      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());

      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("number");
      res.add("var", ctx.varName);

      if (ctx.COLUMNS() != null) {
         res.add("value", ctx.expression().varName + ".shape[1]");
      } else {
         res.add("value", ctx.expression().varName + ".shape[0]");
      }

      return res;
   }

   @Override public ST visitExprPxMultDiv(IMLParser.ExprPxMultDivContext ctx) {
      
      String operand1Res = visit(ctx.e1).render();
      String operand2Res = visit(ctx.e2).render();
      String operand1 = ctx.e1.varName;
      String operand2 = ctx.e2.varName;

      // retira "."
      String rawOp = ctx.op.getText();
      String pyOp  = rawOp.substring(1);

      ctx.varName = newVarName();

      if (ctx.e1.eType instanceof ListType){
         ctx.eType = ctx.e1.eType;
      } else if (ctx.e1.eType.name().equals("image")){
         ctx.eType = ctx.e1.eType;
         ST res = templates.getInstanceOf("pxArithmetic");
         res.add("stat", operand1Res);
         res.add("stat", operand2Res);
         res.add("var", ctx.varName);
         res.add("op", pyOp);
         res.add("list", operand1);
         res.add("exp", operand2);
         return res;
      }
      else{
         ctx.eType = ctx.e2.eType;

         if (ctx.e2.eType.name().equals("image")){
            ST res = templates.getInstanceOf("pxArithmetic");
            res.add("stat", operand1Res);
            res.add("stat", operand2Res);
            res.add("var", ctx.varName);
            res.add("op", pyOp);
            res.add("list", operand2);
            res.add("exp", operand1);
            return res;
         }
      }
      
      ST res = templates.getInstanceOf("assign");

      res.add("stat", operand1Res);
      res.add("stat", operand2Res);

      res.add("var", ctx.varName);
      
      res.add("value", ctx.e1.varName + " " + pyOp + " " + ctx.e2.varName);

      return res;
   }

   @Override public ST visitExprOr(IMLParser.ExprOrContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      ctx.eType = ctx.e1.eType;
      res.add("var", ctx.varName);
      res.add("value", ctx.e1.varName + " or " + ctx.e2.varName);
      return res;
   }

   @Override public ST visitExprNumber(IMLParser.ExprNumberContext ctx) {
      ST res = templates.getInstanceOf("assign");
      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("number");
      res.add("var", ctx.varName);
      res.add("value", ctx.NUMBER().getText());
      return res;
   }

   @Override public ST visitExprCount(IMLParser.ExprCountContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("number");
      res.add("var", ctx.varName);

      ST count = templates.getInstanceOf("count");
      count.add("list", ctx.e2.varName);
      count.add("numb", ctx.e1.varName);
      res.add("value", count.render());
      return res;
   }

   @Override public ST visitExprRead(IMLParser.ExprReadContext ctx) {
    
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("string");

      res.add("value", "input(" + ctx.expression().varName + ")");

      return res;
   }

   @Override public ST visitExprAllAny(IMLParser.ExprAllAnyContext ctx) {
      ST res = templates.getInstanceOf("assign");

      res.add("stat", visit(ctx.expression()).render());

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("boolean");

      res.add("value", ctx.expression().varName + "." + ctx.op.getText() + "()");

      return res;
   }

   @Override public ST visitExprString(IMLParser.ExprStringContext ctx) {
      ST res = templates.getInstanceOf("assign");
      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("string");
      res.add("var", ctx.varName);
      res.add("value", ctx.STRING().getText());
      return res;
   }

   @Override public ST visitExprPxAddSub(IMLParser.ExprPxAddSubContext ctx) {
      
      String operand1Res = visit(ctx.e1).render();
      String operand2Res = visit(ctx.e2).render();
      String operand1 = ctx.e1.varName;
      String operand2 = ctx.e2.varName;

      // retira "."
      String rawOp = ctx.op.getText();
      String pyOp  = rawOp.substring(1);

      ctx.varName = newVarName();


      
      
      if (ctx.e1.eType instanceof ListType){
         ListType leftType = (ListType) ctx.e1.eType;
         if (leftType.getBasicType() instanceof ImageType){
            ST res = templates.getInstanceOf("pxArithmetic");
            if (ctx.e2.eType instanceof NumberType || ctx.e2.eType instanceof PercentageType){
               operand2 += " *255";
            }
            res.add("stat", operand1Res);
            res.add("stat", operand2Res);
            res.add("var", ctx.varName);
            res.add("op", pyOp);
            res.add("list", operand1);
            res.add("exp", operand2);
            return res;
         }
         ctx.eType = ctx.e1.eType;
      }
      else if (ctx.e1.eType.name().equals("image")){
         ctx.eType = ctx.e1.eType;
         ST res = templates.getInstanceOf("pxArithmetic");
         if (ctx.e2.eType instanceof NumberType || ctx.e2.eType instanceof PercentageType){
            operand2 += " *255";
         }
         res.add("stat", operand1Res);
         res.add("stat", operand2Res);
         res.add("var", ctx.varName);
         res.add("op", pyOp);
         res.add("list", operand1);
         res.add("exp", operand2);
         return res;
      }
      else{
         ctx.eType = ctx.e2.eType;
         if (ctx.e2.eType instanceof ImageType){
            ST res = templates.getInstanceOf("pxArithmetic");
            if (ctx.e1.eType instanceof NumberType || ctx.e1.eType instanceof PercentageType){
               operand1 += " *255";
            }
            res.add("stat", operand1Res);
            res.add("stat", operand2Res);
            res.add("var", ctx.varName);
            res.add("op", pyOp);
            res.add("list", operand2);
            res.add("exp", operand1);
            return res;
         }

         ListType rightType = (ListType) ctx.e1.eType;
         if (rightType.getBasicType() instanceof ImageType){
            ST res = templates.getInstanceOf("pxArithmetic");
            if (ctx.e1.eType instanceof NumberType || ctx.e1.eType instanceof PercentageType){
               operand1 += " *255";
            }
            res.add("stat", operand1Res);
            res.add("stat", operand2Res);
            res.add("var", ctx.varName);
            res.add("op", pyOp);
            res.add("list", operand2);
            res.add("exp", operand1);
            return res;
         }
      }
      
      ST res = templates.getInstanceOf("assign");

      res.add("stat", operand1Res);
      res.add("stat", operand2Res);

      res.add("var", ctx.varName);
      
      res.add("value", ctx.e1.varName + " " + pyOp + " " + ctx.e2.varName);

      return res;
   }

   @Override public ST visitExprParenthesis(IMLParser.ExprParenthesisContext ctx) {

      ST child = visit(ctx.expression());

      ctx.varName = ctx.expression().varName;
      ctx.eType = ctx.expression().eType;
      return child;
   }

   @Override public ST visitExprPercentage(IMLParser.ExprPercentageContext ctx) {

      ST res = templates.getInstanceOf("assign");

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("percentage");

      // Retira o '%' do literal
      String raw = ctx.PERCENTAGE().getText();
      String num = raw.substring(0, raw.length() - 1);

      res.add("value", num + " / 100.0");

      return res;
   }
   

   @Override public ST visitExprMorf(IMLParser.ExprMorfContext ctx) {

      ST res = templates.getInstanceOf("assign");
      ST morph = templates.getInstanceOf("morph");

      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("image");

      String op = ctx.op.getText().toUpperCase();
      String flag=null;
      switch (op) {
         case "OPEN":   flag = "cv2.MORPH_OPEN";   break;
         case "CLOSE":  flag = "cv2.MORPH_CLOSE";  break;
         case "DILATE": flag = "cv2.MORPH_DILATE"; break;
         case "ERODE":  flag = "cv2.MORPH_ERODE";  break;
      }

      if (ctx.e1.eType instanceof ListType){
         morph.add("e1", "np.array("+ctx.e1.varName+",np.uint8)");
      } else{
         morph.add("e1", ctx.e1.varName);
      }
      if (ctx.e2.eType instanceof ListType){
         morph.add("e2", "np.array("+ctx.e2.varName+",np.uint8)");
      } else{
         morph.add("e2", ctx.e2.varName);
      }

      morph.add("op", flag);

      res.add("value", morph.render());

      return res;
   }

   @Override public ST visitExprRelational(IMLParser.ExprRelationalContext ctx) {

      ST res = templates.getInstanceOf("assign");

      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("boolean");  

      res.add("value", ctx.e1.varName + " " + ctx.op.getText() + " " + ctx.e2.varName);

      return res;
   }

   @Override public ST visitExprAnd(IMLParser.ExprAndContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("boolean");  
      res.add("var", ctx.varName);
      res.add("value", ctx.e1.varName + " and " + ctx.e2.varName);
      return res;
   }

   @Override public ST visitExprScale(IMLParser.ExprScaleContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("image");  

      String rawOp = ctx.op.getText();
      String value;
      if ("-*".equals(rawOp)) {
         value = "cv2.resize(" + ctx.e1.varName + ", (0, 0), fx=" + ctx.e2.varName + ", fy=1)";
      } else if ("|*".equals(rawOp)) {
         value = "cv2.resize(" + ctx.e1.varName + ", (0, 0), fx=1, fy=" + ctx.e2.varName + ")";
      } else {
         value = "cv2.resize(" + ctx.e1.varName + ", (0, 0), fx=" + ctx.e2.varName + ", fy=" + ctx.e2.varName + ")";
      }
      res.add("value", value);
      return res;

   }

   @Override public ST visitExprList(IMLParser.ExprListContext ctx) {

      ST res = templates.getInstanceOf("assign");

      for (IMLParser.ExpressionContext e : ctx.expression()) {
         res.add("stat", visit(e).render());
      }
      ctx.varName = newVarName();
      res.add("var", ctx.varName);

      if (ctx.firstExpr == null){
         ctx.eType = new ListType(null);
      }
      else{
         ctx.eType = new ListType(ctx.firstExpr.eType);  
      }

      StringBuilder listValue = new StringBuilder("np.array([");
      for (int i = 0; i < ctx.expression().size(); i++) {
         if (i > 0) listValue.append(", ");
         listValue.append(ctx.expression().get(i).varName);
      }
      listValue.append("])");
      res.add("value", listValue.toString());
      return res;

   }

   @Override public ST visitExprPxRelational(IMLParser.ExprPxRelationalContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      ctx.varName = newVarName();
      res.add("var", ctx.varName);

      String operand1 = ctx.e1.varName;
      String operand2 = ctx.e2.varName;
      
      if (ctx.e1.eType instanceof ListType){
         ctx.eType = changeTypeList((ListType) ctx.e1.eType, symbolTable.getTypeByName("boolean"));
      }
      else if (ctx.e2.eType instanceof ListType){
         ctx.eType = changeTypeList((ListType) ctx.e2.eType, symbolTable.getTypeByName("boolean"));
      }
      else{
         // Them it's an image so the type is gonna be an list of list of booleans
         ctx.eType = new ListType(new ListType(symbolTable.getTypeByName("boolean")));
         if (ctx.e1.eType instanceof NumberType){
            operand1 += "*255";
         }else if (ctx.e2.eType instanceof NumberType){
            operand2 += "*255";
         }
      }

      String rawOp = ctx.op.getText();
      String pyOp = rawOp.substring(1);
      res.add("value", operand1 + " " + pyOp + " " + operand2);
      return res;
   }

   @Override public ST visitExprUnary(IMLParser.ExprUnaryContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());

      ctx.eType = ctx.expression().eType;
      
      String sign = ctx.sign.getText();
      String pythonExpr;
      if (sign.equals("-")) {
         pythonExpr = "cv2.flip(" + ctx.expression().varName + ", 0)";
      }
      else if (sign.equals("|")) {
         pythonExpr = "cv2.flip(" + ctx.expression().varName + ", 1)";
      }
      else if (sign.equals(".-")) {
         pythonExpr = "1 -" + ctx.expression().varName;
      } else {
         ST assign = templates.getInstanceOf("assign");
         String varTemp = newVarName();
         assign.add("var", varTemp);
         assign.add("value", "cv2.flip(" + ctx.expression().varName + ", 0)");
         res.add("stat", assign.render());
         pythonExpr = "cv2.flip(" + varTemp + ", 1)";
      }

      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      res.add("value", pythonExpr);

      return res;
   }

   @Override public ST visitExprMultDiv(IMLParser.ExprMultDivContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      
      if (ctx.e1.eType.name().equals("percentage")){
         ctx.eType = ctx.e2.eType;
      }
      else{
         ctx.eType = ctx.e1.eType;
      }
      
      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      res.add("value", ctx.e1.varName + ctx.op.getText() + ctx.e2.varName);
      return res;

   }

   @Override public ST visitExprEqNeq(IMLParser.ExprEqNeqContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.e1).render());
      res.add("stat", visit(ctx.e2).render());
      
      ctx.varName = newVarName();
      ctx.eType = symbolTable.getTypeByName("boolean");
      res.add("var", ctx.varName);
      res.add("value", ctx.e1.varName + ctx.op.getText() + ctx.e2.varName);
      return res;

   }

   @Override public ST visitExprLoad(IMLParser.ExprLoadContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());
      
      ctx.varName = newVarName();
      res.add("var", ctx.varName);
      ctx.eType = symbolTable.getTypeByName("image");


      ST loadImg = templates.getInstanceOf("exprLoad");
      loadImg.add("img", ctx.expression().varName);
      res.add("value", loadImg.render());

      return res;
   }

   @Override public ST visitExprID(IMLParser.ExprIDContext ctx) {
      ctx.varName = getVarName(ctx.ID().getText());
      ctx.eType = symbolTable.resolve(ctx.varName);
      return new ST("");
   }

   @Override public ST visitExprTypeConvertion(IMLParser.ExprTypeConvertionContext ctx) {
      
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression()).render());
      ctx.varName = newVarName();
      ctx.eType = ctx.typeID().res;
      res.add("var", ctx.varName);
      if (ctx.eType instanceof PercentageType){
         res.add("value", ctx.eType.convertionString() + "(" + ctx.expression().varName + ")/100");
      }
      else if (ctx.expression().eType instanceof PercentageType){
         res.add("value", ctx.eType.convertionString() + "(" + ctx.expression().varName + "*100)");
      }
      else if (ctx.expression().eType instanceof ListType || ctx.eType instanceof ImageType){
         ListType listType = (ListType) ctx.expression().eType;
         Type basicElemType = listType.getBasicType();
         if (basicElemType instanceof PercentageType){
            res.add("value", ctx.expression().varName);
         }
         else if (basicElemType instanceof NumberType){
            res.add("value", ctx.expression().varName + "*255");
         }
         else {
            res.add("value", ctx.expression().varName + ".astype(np.uint8)*255");
         }
      }
      else if (ctx.expression().eType instanceof ImageType || ctx.eType instanceof ListType){
         ListType listType = (ListType) ctx.eType;
         Type basicElemType = listType.getBasicType();
         if (basicElemType instanceof PercentageType){
            res.add("value", ctx.expression().varName);
         }
         else if (basicElemType instanceof NumberType){
            res.add("value", ctx.expression().varName + "/255");
         }
         else {
            res.add("value", ctx.expression().varName + "!=0");
         }
      }
      else{
         res.add("value", ctx.eType.convertionString() + "(" + ctx.expression().varName + ")");
      }
      return res;
   }

   @Override public ST visitExprListID(IMLParser.ExprListIDContext ctx) {
      ST res = templates.getInstanceOf("assign");
      res.add("stat", visit(ctx.expression(0)).render());
      ctx.varName = newVarName();
      String value = ctx.expression(0).varName;
      ctx.eType = ctx.expression(0).eType;
      Boolean wasImage = false;
      for (int i=1; i<ctx.expression().size();i++){
         res.add("stat", visit(ctx.expression(i)).render());
         value += "[" + ctx.expression(i).varName +"]";
         if (ctx.eType instanceof ListType){
            ListType subType = (ListType) ctx.eType;
            ctx.eType = subType.getElementType();
         }
         else{
            wasImage=true;
            ctx.eType = symbolTable.getTypeByName("list of number");
         }
      }
      if (wasImage){
         value+="/255";
      }
      res.add("var", ctx.varName);
      res.add("value", value);
      
      return res;
   }

   @Override public ST visitTypeID(IMLParser.TypeIDContext ctx) {
      ST res = null;
      return visitChildren(ctx);
      //return res;
   }
}
