import java.util.*;

@SuppressWarnings("CheckReturnValue")
public class SemanticVisitor extends IMLParserBaseVisitor<List<Exception>> {

   private SymbolTable symbols = new SymbolTable();

   @Override public List<Exception> visitProgram(IMLParser.ProgramContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      /* Check if any semantic errors were collected; */
      for(IMLParser.StatContext stat : ctx.stat()){
         List<Exception> childErrors = visit(stat);

         if (childErrors != null){
            errors.addAll(childErrors);
         }
      }

      /* Return list of errors if any were collected */
      if (!errors.isEmpty()){
         System.err.println("Semantic errors found:");
         for(Exception e : errors){
            System.err.println(e.getMessage());
         }

         System.exit(1);
      }

      return errors;
   }

   @Override public List<Exception> visitInstantiationStat(IMLParser.InstantiationStatContext ctx) {
      return visit(ctx.instantiation());
   }

   @Override public List<Exception> visitAssignmentStat(IMLParser.AssignmentStatContext ctx) {
      return visit(ctx.assignment());
   }

   @Override public List<Exception> visitIfStat(IMLParser.IfStatContext ctx) {
      return visit(ctx.ifStatement());
   }

   @Override public List<Exception> visitUntilStat(IMLParser.UntilStatContext ctx) {
      return visit(ctx.untilLoop());
   }

   @Override public List<Exception> visitForStat(IMLParser.ForStatContext ctx) {
      return visit(ctx.forLoop());
   }

   @Override public List<Exception> visitCommandStat(IMLParser.CommandStatContext ctx) {
      return visit(ctx.command());
   }

   @Override public List<Exception> visitSaveStat(IMLParser.SaveStatContext ctx) {
      return visit(ctx.save());
   }

   @Override public List<Exception> visitListOpStat(IMLParser.ListOpStatContext ctx) {
      return visit(ctx.listOperation());
   }

   @Override public List<Exception> visitInstantiation(IMLParser.InstantiationContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      /* Visit all child nodes first (typeID, assignment, the expression…)
      and merge any errors they might have produced.*/
      List<Exception> exprErrs = visit(ctx.assignment().expression());
      if (exprErrs != null) {
         errors.addAll(exprErrs);
      }

      Type declaredType = ctx.typeID().res;
      String varName = ctx.assignment().ID().getText();

      Type exprType = ctx.assignment().expression().eType;
      if (symbols.isDeclared(varName) && declaredType != null) {
         errors.add(new Exception(
         String.format("Error: Variable '%s' has already been declared!", varName)));
      }

      if (declaredType instanceof ListType && exprType instanceof ListType emptyLit && emptyLit.getElementType() == null) {
         ctx.assignment().expression().eType = declaredType;
         exprType = declaredType;
      }
      else if (!(exprType instanceof ErrorsType) && !(declaredType.equals(exprType)
      || (declaredType instanceof ImageType && isListOfListOfNumberOrPercentage(exprType))
      || (exprType instanceof ImageType && isListOfListOfNumberOrPercentage(declaredType)))) {
         errors.add(new Exception(String.format("Error: Cannot assign %s to %s", exprType, declaredType)));
      }
      symbols.declare(varName, declaredType);

      return errors;
   }

   @Override public List<Exception> visitAssignment(IMLParser.AssignmentContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      // Merge in any nested errors
      List<Exception> childErrs = visit(ctx.expression());
      if (childErrs != null) {
         errors.addAll(childErrs);
      }

      String varName = ctx.ID().getText();
      Type declaredType = symbols.resolve(varName);
      Type exprType = ctx.expression().eType;

      if(declaredType == null){
         errors.add(new Exception(String.format("Error: Variable %s not declared", varName)));
      } 
      else if (!(exprType instanceof ErrorsType) && !(declaredType.equals(exprType)
      || (declaredType instanceof ImageType && isListOfListOfNumberOrPercentage(exprType))
      || (exprType instanceof ImageType && isListOfListOfNumberOrPercentage(declaredType)))) {
         errors.add(new Exception(String.format("Error: Cannot assign %s to %s", exprType, declaredType)));
      }
      
      return errors;
   }

   @Override public List<Exception> visitPrintCommand(IMLParser.PrintCommandContext ctx) {
      List<Exception> errors = visit(ctx.expression());

      if (errors == null){
         return new ArrayList<Exception>();
      }

      return errors;
   }

   @Override public List<Exception> visitDrawCommand(IMLParser.DrawCommandContext ctx) {
      List<Exception> errors =  visit(ctx.expression());

      if (errors == null){
         errors = new ArrayList<Exception>();
      }

      Type t = ctx.expression().eType;

      boolean image = t instanceof ImageType || isListOfListOfNumberOrPercentage(t);
      boolean imageList = t instanceof ListType && ((ListType) t).getElementType() instanceof ImageType;

      if (!image && !imageList){
         errors.add(new Exception(String.format("Error: 'draw' expects an image or list of images, instead found %s", t)));
      }

      return errors;
   }

   @Override public List<Exception> visitSave(IMLParser.SaveContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> erros1 = visit(ctx.e1);
      List<Exception> erros2 = visit(ctx.e2);
      if (erros1 != null) errors.addAll(erros1);
      if (erros2 != null) errors.addAll(erros2);

      Type sourceType = ctx.e1.eType;
      Type destType   = ctx.e2.eType;

      boolean image  = sourceType instanceof ImageType;
      boolean imageNum = isListOfListOfNumberOrPercentage(sourceType);
      boolean imageList  = sourceType instanceof ListType && ((ListType) sourceType).getElementType() instanceof ImageType;

      if (!image && !imageList && !imageNum) {
         errors.add(new Exception(String.format("Error: 'save' source must be image or list of images, instead found %s", sourceType)));
      }

      if (!(destType instanceof StringType)) {
         errors.add(new Exception(String.format("Error: 'save' destination must be a string, instead found %s", destType)));
      }

      return errors;
   }

   @Override public List<Exception> visitIfStatement(IMLParser.IfStatementContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      // Visit the expression so that any sub-expression errors are collected
      List<Exception> nested = visit(ctx.expression());
      if (nested != null) errors.addAll(nested);

      List<Exception> thenErrors = visit(ctx.thenBlock);
      if (thenErrors != null) errors.addAll(thenErrors);

      if (ctx.elseBlock != null) {
        List<Exception> elseErrors = visit(ctx.elseBlock);
        if (elseErrors != null) errors.addAll(elseErrors);
      }

      if ( !(ctx.expression().eType instanceof BooleanType)) {
         errors.add(new Exception(String.format("Error: 'if' condition must be boolean, instead found %s", ctx.expression().eType)));
      }

      return errors;
   }

   @Override public List<Exception> visitUntilLoop(IMLParser.UntilLoopContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.expression()));

      if( ctx.expression() == null || !(ctx.expression().eType instanceof BooleanType)){
         errors.add(new Exception(String.format("Error: 'until' condition must be boolean, instead found %s", ctx.expression().eType)));
      }

      if (ctx.statBlock() != null) {
        List<Exception> bodyErrors = visit(ctx.statBlock());
        if (bodyErrors != null) errors.addAll(bodyErrors);
      }

      return errors;
   }

   @Override public List<Exception> visitForLoop(IMLParser.ForLoopContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> withinErrors = visit(ctx.expression());
      if (withinErrors != null){

         errors.addAll(withinErrors);
      } 

      Type boundType = ctx.expression().eType;
      Type loopVarType = ctx.typeID().res;

      if (!(boundType instanceof ListType && ((ListType) boundType).getElementType().equals(loopVarType)))  {
         errors.add(new Exception(String.format("Error: 'within' expression must be a list of %s, instead found %s",loopVarType, boundType)));
      }

      String loopVar = ctx.ID().getText();
      if (!symbols.declare(loopVar, loopVarType)) {
        errors.add(new Exception(String.format("Error: Variable '%s' has already been declared!", loopVar)));
      }

      for (IMLParser.StatContext st : ctx.statBlock().stat()) {
         List<Exception> bodyErrs = visit(st);
         if (bodyErrs != null) {
           errors.addAll(bodyErrs);
         }
      }

      return errors;
  }

   @Override public List<Exception> visitListOperation(IMLParser.ListOperationContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> errors1 = visit(ctx.e1);
      List<Exception> errors2 = visit(ctx.e2);

      if(errors1 != null){
         errors.addAll(errors1);
      }
      if(errors2 != null){
         errors.addAll(errors2);
      }

      Type listType = ctx.e1.eType;
      Type elemType = null;

      if(!(listType instanceof ListType)){
         errors.add(new Exception("Error: Left operand must be a list."));
         return errors;
      }else{
         elemType = ((ListType) listType).getElementType();
      }

      String op = ctx.op.getText();
      if(!op.equals("pop")){
         if(!areTypesCompatible(elemType, ctx.e2.eType)){
            errors.add(new Exception(String.format("Error: Operation %s requires element of type %s, but instead found %s.", op, elemType, ctx.e2.eType)));
         }
      }

      return errors;
   }

   @Override public List<Exception> visitExprReverse(IMLParser.ExprReverseContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.expression()));
      Type exprType = ctx.expression().eType;

      if (exprType instanceof BooleanType) {
         ctx.eType = new BooleanType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator NOT requires a boolean expression, but got: " + exprType));
      }

      return errors;
   }

   @Override public List<Exception> visitExprRun(IMLParser.ExprRunContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.expression()));

      Type exprType = ctx.expression().eType;

      if (exprType instanceof StringType) {
         ctx.eType = new ImageType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: The 'run from' instruction requires a string (path), but got: " + exprType));
      }

      return errors;
   }

   @Override public List<Exception> visitExprBoolean(IMLParser.ExprBooleanContext ctx) {
      List<Exception> errors = new ArrayList<>();
      ctx.eType = new BooleanType();
      return errors;
   }

   @Override public List<Exception> visitExprHats(IMLParser.ExprHatsContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;

      String op = ctx.op.getText();

      if ((left instanceof ImageType && right instanceof ImageType) ||  // imagem com imagem
              (left instanceof ImageType && isListOfListOfNumberOrPercentage(right)) ||  // imagem com lista de listas de números ou percentagens
              (right instanceof ImageType && isListOfListOfNumberOrPercentage(left)) ||  // lista de listas com imagem
              (isListOfListOfNumberOrPercentage(left) && isListOfListOfNumberOrPercentage(right))) {  // lista de listas com lista de listas
         ctx.eType = new ImageType(); // A operação é válida para ambos os casos
      } else {
         ctx.eType = new ErrorsType(); // Caso contrário, marca erro
         errors.add(new Exception("Error: Operator " + op + " hat requires both operands to be images or lists of lists of numbers/percentages, but got: " + left + " and " + right));
      }

      return errors;
   }


   @Override public List<Exception> visitExprAddSub(IMLParser.ExprAddSubContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      //Adicionar os erros de baixo
      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();

      boolean leftNum = left instanceof NumberType || left instanceof PercentageType;
      boolean rightNum = right instanceof NumberType || right instanceof PercentageType;

      if (left instanceof NumberType && right instanceof NumberType) {
         ctx.eType = new NumberType();
         return errors;
      }

      if (left instanceof PercentageType && right instanceof PercentageType) {
         ctx.eType = symbols.getTypeByName("percentage");
         return errors;
      }

      if (op.equals("+") && left instanceof StringType && right instanceof StringType) {
         ctx.eType = new StringType();
         return errors;
      }

      ctx.eType = new ErrorsType();
      errors.add(new Exception("Error: Invalid operands " + op + ": " + left + " and " + right));

      return errors;
   }

   @Override public List<Exception> visitExprPxEqNeq(IMLParser.ExprPxEqNeqContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type leftType = ctx.e1.eType;
      Type rightType = ctx.e2.eType;

      if (!isImageLike(leftType) || !isImageLike(rightType)) {
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error: Operator '%s' requires operands for pixel-wise operation, but got: %s and %s", ctx.op.getText(), leftType, rightType)));
         return errors;
      }

      if (!haveSameStructure(leftType, rightType)) {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operands have incompatible structures for pixel-wise operation"));
         return errors;
      }

      // Define o tipo da expressão resultante como uma lista de números (NumberType)
      ctx.eType = new ListType(new ListType(new NumberType()));

      return errors;
   }

   private boolean isImageLike(Type t) {
      if (t instanceof ImageType) return true;
      if (t instanceof ListType lt) {
         return isImageLike(lt.getElementType());
      }
      return (t instanceof NumberType) || (t instanceof PercentageType);
   }

   private boolean haveSameStructure(Type t1, Type t2) {
      if (t1 == null || t2 == null) return false;

      if (t1 instanceof ListType lt1 && t2 instanceof ListType lt2) {
         return haveSameStructure(lt1.getElementType(), lt2.getElementType());
      }

      // Para tipos base considera equivalentes se forem do mesmo tipo base (Number, Percentage, Boolean etc)
      return t1.getClass().equals(t2.getClass());
   }

   @Override public List<Exception> visitExprColumns(IMLParser.ExprColumnsContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.expression()));

      Type exprType = ctx.expression().eType;
      String op = ctx.getStart().getText();

      boolean exprTypeOK = exprType instanceof ImageType || isListOfListOfNumberOrPercentage(exprType);

      if (exprTypeOK) {
         ctx.eType = new NumberType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator '" + op + " of' requires an image operand, but got: " + exprType));
      }

      return errors;
   }

   @Override public List<Exception> visitExprPxMultDiv(IMLParser.ExprPxMultDivContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();

      if ((left instanceof ImageType && right instanceof ImageType) || 
         ((isListOfListOfNumberOrPercentage(left) || left instanceof NumberType || left instanceof PercentageType) && right instanceof ImageType)
         || ((isListOfListOfNumberOrPercentage(right) || right instanceof NumberType || right instanceof PercentageType) && left instanceof ImageType)) {
         ctx.eType = symbols.getTypeByName("image");
         return errors;
      }
      
      if (left instanceof ListType && right instanceof ListType) {
         ListType leftList = (ListType) left;
         ListType rightList = (ListType) right;

         int dimensionL = leftList.countDimensions();
         int dimensionR = rightList.countDimensions();
         
         Type basiTypeL = leftList.getBasicType();
         Type basiTypeR = rightList.getBasicType();

         if (basiTypeL instanceof ImageType){
            dimensionL+=2;
         }
         if (basiTypeR instanceof ImageType){
            dimensionR+=2;
         }

         if (dimensionL!=dimensionR){
            ctx.eType = new ErrorsType();
            errors.add(new Exception("Operator " + op + " requires lists of the same dimension, but got: " + left + " and " + right));
            return errors;
         }
         if (basiTypeL instanceof ImageType && ( basiTypeR instanceof NumberType ||  basiTypeR instanceof PercentageType)){
            ctx.eType = left;
            return errors;
         }
         if ((basiTypeR instanceof ImageType) && (basiTypeL instanceof NumberType || basiTypeL instanceof PercentageType)){
            ctx.eType = right;
            return errors;
         }
         if ((basiTypeR instanceof NumberType || basiTypeR instanceof PercentageType) && (basiTypeL instanceof NumberType || basiTypeL instanceof PercentageType)){
            ctx.eType = right;
            return errors;
         }

         
      } else if (left instanceof ListType && ( right instanceof NumberType || right instanceof PercentageType)) {
         ListType leftList = (ListType) left;

         ctx.eType = left;
         return errors;
         
      } else if (right instanceof ListType && ( left instanceof NumberType || left instanceof PercentageType)) {
         ListType rightList = (ListType) right;
         
         ctx.eType = right;
         return errors;

      }
      
      ctx.eType = new ErrorsType();
      errors.add(new Exception("Operator " + op + " doesn't support operations between " + left + " and " + right));
      
      return  errors;
   }

   @Override public List<Exception> visitExprOr(IMLParser.ExprOrContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> errorsLeft = visit(ctx.e1);
      List<Exception> errorsRight = visit(ctx.e2);

      if (errorsLeft != null){
         errors.addAll(errorsLeft);
      }
      if(errorsRight != null){
         errors.addAll(errorsRight);
      }

      Type leftType = ctx.e1.eType;
      Type rightType = ctx.e2.eType;

      if(leftType instanceof BooleanType && rightType instanceof BooleanType){
         ctx.eType = new BooleanType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error : OR operator requires boolean operands, instead found %s and %s", leftType, rightType)));
      }
      
      return errors;
   }

   @Override public List<Exception> visitExprNumber(IMLParser.ExprNumberContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      ctx.eType = new NumberType();
      return errors;
   }

   @Override public List<Exception> visitExprCount(IMLParser.ExprCountContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> errors1 = visit(ctx.e1);
      List<Exception> errors2 = visit(ctx.e2);

      if (errors1 != null){
         errors.addAll(errors1);
      }
      if(errors2 != null){
         errors.addAll(errors2);
      }

      Type type1 = ctx.e1.eType;
      Type type2 = ctx.e2.eType;

      if(!(type1 instanceof NumberType)){
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error: COUNT PIXEL expects a number predicate for e1, instead found %s", type1)));
      }

      if (!(type2 instanceof ImageType || isListOfListOfNumberOrPercentage(type2)) && !(type2 instanceof ListType && ((ListType) type2).getElementType() instanceof ImageType)){
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error: COUNT PIXEL expects image or list of images for e2, instead found %s", type2)));
      }
       else {
         ctx.eType = new NumberType();
      }

      return errors;
   }

   @Override public List<Exception> visitExprRead(IMLParser.ExprReadContext ctx) {
      List<Exception> errors = visit(ctx.expression());

      if (errors == null){
         errors = new ArrayList<Exception>();
      }

      Type type = ctx.expression().eType;

      if ( !(type instanceof StringType)){
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error: 'read' expects a string (filename), instead found %s", type)));
      }else{
         ctx.eType = new StringType();
      }

      return errors;
   }

   @Override public List<Exception> visitExprString(IMLParser.ExprStringContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      ctx.eType = new StringType();
      return errors;
   }

   @Override public List<Exception> visitExprPxAddSub(IMLParser.ExprPxAddSubContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();


      if ((left instanceof ImageType && right instanceof ImageType) || 
         ((isListOfListOfNumberOrPercentage(left) || left instanceof NumberType || left instanceof PercentageType) && right instanceof ImageType)
         || ((isListOfListOfNumberOrPercentage(right) || right instanceof NumberType || right instanceof PercentageType) && left instanceof ImageType)) {
         ctx.eType = symbols.getTypeByName("image");
         return errors;
      }
      if (left instanceof ListType && right instanceof ListType) {
         ListType leftList = (ListType) left;
         ListType rightList = (ListType) right;

         int dimensionL = leftList.countDimensions();
         int dimensionR = rightList.countDimensions();
         
         Type basiTypeL = leftList.getBasicType();
         Type basiTypeR = rightList.getBasicType();

         if (basiTypeL instanceof ImageType){
            dimensionL+=2;
         }
         if (basiTypeR instanceof ImageType){
            dimensionR+=2;
         }

         if (dimensionL!=dimensionR){
            ctx.eType = new ErrorsType();
            errors.add(new Exception("Operator " + op + " requires lists of the same dimension, but got: " + left + " and " + right));
            return errors;
         }
         if (basiTypeL instanceof ImageType && ( basiTypeR instanceof NumberType ||  basiTypeR instanceof PercentageType)){
            ctx.eType = left;
            return errors;
         }
         if ((basiTypeR instanceof ImageType) && (basiTypeL instanceof NumberType || basiTypeL instanceof PercentageType)){
            ctx.eType = right;
            return errors;
         }
         if (!basiTypeL.equals(basiTypeR)) {
            ctx.eType = new ErrorsType();
            errors.add(new Exception("Operator " + op + " requires both lists to have the same subtype, but got: " + left + " and " + right));
            return errors;
         }
         if (basiTypeL instanceof BooleanType || basiTypeL instanceof StringType){
            ctx.eType = new ErrorsType();
            errors.add(new Exception("Operator " + op + " doesn't suport lists with the subtype: " + left));
            return errors;
         }
         if (basiTypeR instanceof BooleanType || basiTypeR instanceof StringType){
            ctx.eType = new ErrorsType();
            errors.add(new Exception("Operator " + op + " doesn't suport lists with the subtype: " + right));
            return errors;
         }
         else{
            ctx.eType = left;
            return errors;
         }
      } else if (left instanceof ListType && (right instanceof NumberType || right instanceof PercentageType)) {
         ListType leftList = (ListType) left;
         
         if (leftList.getBasicType().equals(right)){
            ctx.eType = left;
            return errors;
         }
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Operator " + op + " with " + left + " requires element of subtype, but got: " + right));
         return errors;
      } else if (right instanceof ListType && (left instanceof NumberType || left instanceof PercentageType)) {
         ListType rightList = (ListType) right;
         
         if (rightList.getBasicType().equals(left)){
            ctx.eType = right;
            return errors;
         }
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Operator " + op + " with " + right + " requires element of subtype, but got: " + left));
         return errors;
      }
      
      ctx.eType = new ErrorsType();
      errors.add(new Exception("Operator " + op + " doesn't support operations between " + left + " and " + right));
      
      return  errors;
   }

   @Override public List<Exception> visitExprParenthesis(IMLParser.ExprParenthesisContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.expression()));

      ctx.eType = ctx.expression().eType;
      return errors;
   }

   @Override public List<Exception> visitExprPercentage(IMLParser.ExprPercentageContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      ctx.eType = new PercentageType();
      return errors;
   }

   @Override public List<Exception> visitExprMorf(IMLParser.ExprMorfContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();

      boolean leftOK = left instanceof ImageType || isListOfListOfNumberOrPercentage(left);
      boolean rightOK = right instanceof ImageType || isListOfListOfNumberOrPercentage(right);

      if (leftOK && rightOK) {
         ctx.eType = new ImageType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Morphological operator " + op + " requires both operands to be images, but got: " + left + " and " + right));
      }

      return errors;
   }

   @Override public List<Exception> visitExprRelational(IMLParser.ExprRelationalContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();

      boolean leftOp = left instanceof NumberType || left instanceof PercentageType;
      boolean rightOp = right instanceof NumberType || right instanceof PercentageType;

      if (leftOp && rightOp) {
         ctx.eType = new BooleanType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator " + op + " requires numeric operands, but got: " + left + " and " + right));
      }

      return errors;
   }

   @Override public List<Exception> visitExprAnd(IMLParser.ExprAndContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> errorsLeft = visit(ctx.e1);
      List<Exception> errorsRight = visit(ctx.e2);

      if (errorsLeft != null){
         errors.addAll(errorsLeft);
      }
      if(errorsRight != null){
         errors.addAll(errorsRight);
      }

      Type leftType = ctx.e1.eType;
      Type rightType = ctx.e2.eType;

      if(leftType instanceof BooleanType && rightType instanceof BooleanType){
         ctx.eType = new BooleanType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error : AND operator requires boolean operands, instead found %s and %s", leftType, rightType)));
      }
      
      return errors;
   }

   @Override public List<Exception> visitExprScale(IMLParser.ExprScaleContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> errorsLeft = visit(ctx.e1);
      List<Exception> errorsRight = visit(ctx.e2);

      if (errorsLeft != null){ 
         errors.addAll(errorsLeft);
      }
      if (errorsRight != null) {
         errors.addAll(errorsRight);
      }

      Type leftType = ctx.e1.eType;
      Type rightType = ctx.e2.eType;
      String opText = ctx.op.getText();

      if (!(leftType instanceof ImageType || isListOfListOfNumberOrPercentage(leftType))) {
         errors.add(new Exception(String.format("Error: left operand of %s must be image, instead found %s", opText, leftType)));
      }

      if (!(rightType instanceof NumberType)) {
         errors.add(new Exception(String.format("Error: right operand of %s must be number, instead found %s", opText, rightType)));
      } 

      if(errors.isEmpty()){
         ctx.eType = new ImageType();
      } 

      return errors;
   }

   @Override public List<Exception> visitExprList(IMLParser.ExprListContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      if (ctx.expression().isEmpty()) {

         ctx.eType = new ListType(null);
         return errors;
      }

      List<Type> elementTypes = new ArrayList<>();
      for (IMLParser.ExpressionContext exprCtx : ctx.expression()) {
         List<Exception> childErrors = visit(exprCtx);
         
         if (childErrors != null) {
            errors.addAll(childErrors);
         }
         
         elementTypes.add(exprCtx.eType);
      }

      Type firstType = elementTypes.get(0);
      for (Type t : elementTypes) {
         if (t == null) {
            errors.add(new Exception("Error: List element has undefined type."));
            ctx.eType = new ErrorsType();
            return errors;
         }
         if (!t.equals(firstType)) {
            errors.add(new Exception("Error: All elements of the list must have the same type."));
            ctx.eType = new ErrorsType();
            return errors;
         }
      }

      ctx.eType = new ListType(firstType);

      return errors;
   }

   @Override public List<Exception> visitExprPxRelational(IMLParser.ExprPxRelationalContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();

      boolean leftImage = left instanceof ImageType || isListOfListOfNumberOrPercentage(left);
      boolean rightImage = right instanceof ImageType || isListOfListOfNumberOrPercentage(right);
      boolean leftScalar = left instanceof NumberType || left instanceof PercentageType;
      boolean rightScalar = right instanceof NumberType || right instanceof PercentageType;

      boolean valid = (leftImage && (rightImage || rightScalar)) || (rightImage && rightScalar);

      if (valid) {
         ctx.eType = new ImageType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator '" + op + "' requires image combined with image or scalar, but got: " + left + " and " + right));
      }

      return errors;
   }

   @Override public List<Exception> visitExprUnary(IMLParser.ExprUnaryContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.expression()));
      Type operand = ctx.expression().eType;
      String op = ctx.sign.getText();

      boolean operandOK = operand instanceof ImageType || isListOfListOfNumberOrPercentage(operand);

      if(operandOK) {
         ctx.eType = new ImageType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Unary operator " + op + " only applies to image operands, but got: " + operand));
      }

      return errors;
   }

   @Override public List<Exception> visitExprMultDiv(IMLParser.ExprMultDivContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;

      boolean leftOk = left instanceof NumberType || left instanceof PercentageType;
      boolean rightOk = right instanceof NumberType || right instanceof PercentageType;

      if (leftOk && rightOk) {
         ctx.eType = new NumberType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator " + ctx.op.getText() + " requires numeric (number or percentage) operands, got: "
         + left + " and " + right));
      }

      return errors;
   }

   @Override public List<Exception> visitExprEqNeq(IMLParser.ExprEqNeqContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.e1));
      errors.addAll(visit(ctx.e2));

      Type left = ctx.e1.eType;
      Type right = ctx.e2.eType;
      String op = ctx.op.getText();

      boolean compatible = left.getClass().equals(right.getClass()) ||
              ((left instanceof NumberType || left instanceof PercentageType) &&
                      (right instanceof NumberType || right instanceof PercentageType));

      if (compatible) {
         ctx.eType = new BooleanType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator " + op + " requires compatible types, but got: " + left + " and " + right));
      }

      return errors;
   }

   @Override public List<Exception> visitExprLoad(IMLParser.ExprLoadContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();
      errors.addAll(visit(ctx.expression()));

      Type exprType = ctx.expression().eType;

      if (exprType instanceof StringType) {
         ctx.eType = new ImageType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operation 'load from' expects string path, but got: " + exprType));
      }

      return errors;
   }

   @Override public List<Exception> visitExprID(IMLParser.ExprIDContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      String varName = ctx.getText();
      Type type = symbols.resolve(varName);

      if(type == null) {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Variable " + varName + " not declared!"));
      } else {
         ctx.eType = type;
         ctx.varName = varName;
      }

      return errors;
   }

   @Override public List<Exception> visitExprTypeConvertion(IMLParser.ExprTypeConvertionContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      List<Exception> innerErrors = visit(ctx.expression());
      if (innerErrors != null) {
         errors.addAll(innerErrors);
      }

      Type targetType = ctx.typeID().res;   
      Type exprType = ctx.expression().eType; 

      if (exprType != null && targetType != null) {
         boolean validConversion = false;

         if (exprType.equals(targetType)){
            validConversion = true;
         }
         else if ((exprType instanceof NumberType && targetType instanceof PercentageType) || (exprType instanceof PercentageType && targetType instanceof NumberType)){
            validConversion = true;
         }
         else if ((exprType instanceof NumberType && targetType instanceof StringType) || (exprType instanceof StringType && targetType instanceof NumberType)) {
            validConversion = true;
         }
         else if ((exprType instanceof PercentageType && targetType instanceof StringType) || (exprType instanceof StringType && targetType instanceof PercentageType)) {
            validConversion = true;
         }
         else if ((exprType instanceof BooleanType && targetType instanceof StringType) || (exprType instanceof StringType && targetType instanceof BooleanType)) {
            validConversion = true;
         }else if ((targetType instanceof ImageType && isListOfListOfNumberOrPercentage(exprType))
         || (exprType instanceof ImageType && isListOfListOfNumberOrPercentage(targetType))) {
             validConversion = true;
         }  
         else if (exprType instanceof ListType && targetType instanceof ListType) {
            validConversion = checkListConversion((ListType) exprType, (ListType) targetType);
         }
         else if (exprType instanceof ImageType && targetType instanceof StringType) {
            validConversion = true;
         }

         if (validConversion) {
            ctx.eType = targetType;
         } else {
            ctx.eType = new ErrorsType();
            errors.add(new Exception(String.format("Error: Invalid type conversion from %s to %s", exprType, targetType)));
         }
      } else{
         ctx.eType = new ErrorsType();
         errors.add(new Exception(String.format("Error: Invalid type conversion with null type (from %s to %s)", exprType, targetType)));
      }

      return errors;
   }

   @Override public List<Exception> visitTypeID(IMLParser.TypeIDContext ctx) {
      return visitChildren(ctx);
   }

   @Override public List<Exception> visitExprAllAny(IMLParser.ExprAllAnyContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      errors.addAll(visit(ctx.expression()));
      Type exprType = ctx.expression().eType;
      String op = ctx.op.getText();

      boolean exprTypeOK = exprType instanceof ImageType || isListOfListOfNumberOrPercentage(exprType);

      if (exprTypeOK) {
         ctx.eType = new BooleanType();
      } else {
         ctx.eType = new ErrorsType();
         errors.add(new Exception("Error: Operator '" + op + " pixel' requires an image, but got: " + exprType));
      }

      return errors;
   }

   @Override public List<Exception> visitExprListID(IMLParser.ExprListIDContext ctx) {
      List<Exception> errors = new ArrayList<Exception>();

      visit(ctx.expression(0)); 
      
      Type listType = ctx.expression(0).eType;
      int indexSize = ctx.expression().size();
      if (!(listType instanceof ListType || listType instanceof ImageType) || listType == null) {
         errors.add(new Exception("Error: Cannot index non-list type: " + listType));
         ctx.eType = new ErrorsType();
      }

      Type toReturn = listType;

      for (int i=1; i < indexSize; i++) {
         errors.addAll(visit(ctx.expression(i)));

         Type indexType = ctx.expression(i).eType;
         if (indexType instanceof NumberType && listType instanceof ImageType) {
           toReturn = symbols.getTypeByName("list of numbers");
         }
         else if (indexType instanceof NumberType && listType instanceof ListType) {
            toReturn = ((ListType) toReturn).getElementType();
          }else {
            errors.add(new Exception("Error: The index must be a number"));
            ctx.eType = new ErrorsType();
            return errors;
         }
      }

      ctx.eType = toReturn;

      return errors;
   }


   //Função auxiliar
   private boolean isListOfListOfNumberOrPercentage(Type type) {
      if (type instanceof ListType) {
         Type elementType = ((ListType) type).getElementType();
         // Verifica se o tipo de elemento é uma lista de números ou percentagens
         if (elementType instanceof ListType) {
            Type innerElementType = ((ListType) elementType).getElementType();
            return (innerElementType instanceof NumberType || innerElementType instanceof PercentageType);
         }
      }
      return false;
   }

   //Função auxiliar
   private boolean areTypesCompatible(Type expected, Type actual) {
    // Considera equivalência entre ImageType e listas matrizes
    if (isImageOrEquivalent(expected) && isImageOrEquivalent(actual)) {
        return true;
    }
    
    if (expected.equals(actual)) {
        return true;
    }
    
    if (expected instanceof ListType expectedList && actual instanceof ListType actualList) {
        return areTypesCompatible(expectedList.getElementType(), actualList.getElementType());
    }
    
    return false;
   }

   //Função auxiliar
   private boolean isImageOrEquivalent(Type t) {
      if (t instanceof ImageType) {
         return true;
      }
      if (t instanceof ListType lt) {
         Type inner = lt.getElementType();
         if (inner instanceof ListType innerList) {
               Type innerMost = innerList.getElementType();
               return innerMost instanceof PercentageType;
         }
      }
      return false;
   }

   //Função auxiliar
   private boolean checkListConversion(ListType fromList, ListType toList) {
      Type fromElem = fromList.getElementType();
      Type toElem = toList.getElementType();

      if (fromElem.equals(toElem)) {
         return true;
      }

      // Conversões implícitas básicas
      if ((fromElem instanceof NumberType && toElem instanceof PercentageType) ||
         (fromElem instanceof PercentageType && toElem instanceof NumberType)) {
         return true;
      }

      // Se ambos forem listas, chamada recursiva
      if (fromElem instanceof ListType fromSubList && toElem instanceof ListType toSubList) {
         return checkListConversion(fromSubList, toSubList);
      }
      return false;
   }
}