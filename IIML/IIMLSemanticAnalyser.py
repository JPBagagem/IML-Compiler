from antlr4 import *
from IIMLParser import IIMLParser
from IIMLParserVisitor import IIMLParserVisitor

class IIMLSemanticAnalyser(IIMLParserVisitor):

   def __init__(self):
      self.env = {}
      self.image_created = False

   def visitProgram(self, ctx:IIMLParser.ProgramContext):
      return self.visitChildren(ctx)

   def visitStat(self, ctx:IIMLParser.StatContext):
      return self.visitChildren(ctx)

   def visitVarDeclStmt(self, ctx:IIMLParser.VarDeclStmtContext):
      return self.visitChildren(ctx)

   def visitCreateImageStmt(self, ctx:IIMLParser.CreateImageStmtContext):
      
      if self.image_created:
            raise RuntimeError("Imagem já foi criada. Não é permitido criar uma segunda imagem.")
        
      self.image_created = True

   def visitPlaceShapeStmt(self, ctx:IIMLParser.PlaceShapeStmtContext):
      if not self.image_created: 
            return RuntimeError("Imagem ainda não foi criada.")
        
      self.image_created = True

   def visitCreateFormStmt(self, ctx:IIMLParser.CreateFormStmtContext):
      
      if ctx.CIRCLE():
            
         if len(ctx.expr()) != 1:
               return ValueError("Forma 'circle' requer exatamente 1 argumento.")

         radius = int(self.visit(ctx.expr(0)))

         if radius <= 0:
            return ValueError(f"O raio do círculo deve ser maior que zero (recebido: {radius})")
      
      elif ctx.RECTANGLE():
         
         if len(ctx.expr()) != 2:
            return ValueError("Forma 'rectangle' requer exatamente 2 argumentos.")

         width = int(self.visit(ctx.expr(0)))
         height = int(self.visit(ctx.expr(1)))

         if width <= 0 or height <= 0:
            return ValueError(f"Dimensões inválidas para retângulo: largura={width}, altura={height}")
         
      elif ctx.CROSS():
         
         if len(ctx.expr()) == 1:
            size = int(self.visit(ctx.expr(0)))
            thickness = 1

         elif len(ctx.expr()) == 2:
            if ctx.WITH() is None or ctx.THICKNESS() is None:
               return ValueError("Forma 'cross' com 2 argumentos requer 'with thickness'.")
            
            size = int(self.visit(ctx.expr(0)))
            thickness = int(self.visit(ctx.expr(1)))

         else:
            return ValueError("Forma 'cross' requer 1 argumento (tamanho) ou 2 com 'with thickness'.")

         size = int(self.visit(ctx.expr(0)))
         thickness = int(self.visit(ctx.expr(1))) if len(ctx.expr()) > 1 else 1

         if size <= 0 or thickness <= 0:
            return ValueError(f"Tamanho ou espessura inválidos para cruz: tamanho={size}, espessura={thickness}")

      elif ctx.PLUS():

         num_args = len(ctx.expr())
         has_thickness = ctx.WITH() is not None and ctx.THICKNESS() is not None

         if num_args == 1:
            size = int(self.visit(ctx.expr(0)))
            thickness = 1

         elif num_args == 2:
            if not has_thickness:
               return ValueError("Forma 'plus' com 2 argumentos requer 'with thickness'.")
            
            size = int(self.visit(ctx.expr(0)))
            thickness = int(self.visit(ctx.expr(1)))

         else:
            return ValueError("Forma 'plus' requer 1 argumento (tamanho) ou 2 com 'with thickness'.")

         if size <= 0 or thickness <= 0:
            return ValueError(
               f"Tamanho ou espessura inválidos para 'plus': tamanho={size}, espessura={thickness}"
            )


      elif ctx.LINE():

         def resolve_coord(line_ctx, index):
               var_ctx = line_ctx.expr(index)
               val = self.visit(var_ctx)

               if not isinstance(val, (int, float)):
                  return TypeError(f"Coordenada '{var_ctx.getText()}' não é numérica.")

         try:
            x1 = resolve_coord(ctx.lineExpr(0), 0)
            y1 = resolve_coord(ctx.lineExpr(0), 1)
            x2 = resolve_coord(ctx.lineExpr(1), 0)
            y2 = resolve_coord(ctx.lineExpr(1), 1)

         except Exception as e:
            return ValueError(f"Erro nas coordenadas da linha: {e}")


         thickness = 1
         if len(ctx.expr()) == 1:
            thickness_val = self.visit(ctx.expr(0))

            if not isinstance(thickness_val, (int, float)) or thickness_val <= 0:
               return ValueError("Espessura da linha deve ser um número positivo.")


      elif ctx.STAR():
         expr_count = len(ctx.expr())
         
         if expr_count == 0:
            return ValueError("Forma 'star' requer pelo menos um argumento (raio).")
         
         if expr_count > 2:
            return ValueError("Forma 'star' aceita no máximo 2 argumentos (raio e espessura opcional).")

         radius_val = self.visit(ctx.expr(0))
         
         if not isinstance(radius_val, (int, float)) or radius_val <= 0:
            return ValueError("Raio da forma 'star' deve ser um número positivo.")

         thickness = 1
         if expr_count == 2:
            thickness_val = self.visit(ctx.expr(1))
      
            if not isinstance(thickness_val, (int, float)) or thickness_val <= 0:
               return ValueError("Espessura da forma 'star' deve ser um número positivo.")

      return NotImplementedError("Forma ainda não suportada.")

   def visitLineExpr(self, ctx:IIMLParser.LineExprContext):
      return self.visitChildren(ctx)

   def visitStringExpr(self, ctx:IIMLParser.StringExprContext):
      return self.visitChildren(ctx)

   def visitMulDivExpr(self, ctx:IIMLParser.MulDivExprContext):

      left = self.visit(ctx.e1)
      right = self.visit(ctx.e2)

      if not isinstance(left, (int, float)) or not isinstance(right, (int, float)):
         return TypeError(f"Operações aritméticas requerem operandos numéricos (recebidos: {type(left)}, {type(right)})")

      if ctx.op.type == ctx.DIV and isinstance(right, (int, float)) and right == 0:
         return ZeroDivisionError("Divisão por zero detectada em tempo de análise semântica.")


   def visitIdExpr(self, ctx:IIMLParser.IdExprContext):
      name = ctx.getText()
        
      if name not in self.env:
         return NameError(f"Variável '{name}' não foi declarada.")

   def visitNumberExpr(self, ctx:IIMLParser.NumberExprContext):
      return self.visitChildren(ctx)

   def visitParenExpr(self, ctx:IIMLParser.ParenExprContext):
      return self.visitChildren(ctx)

   def visitReadStrExpr(self, ctx:IIMLParser.ReadStrExprContext):
      return self.visitChildren(ctx)

   def visitAddSubExpr(self, ctx:IIMLParser.AddSubExprContext):
      return self.visitChildren(ctx)

   def visitConvExpr(self, ctx:IIMLParser.ConvExprContext):

      type_name = ctx.typeID().getText()

      if type_name not in ["number", "string", "boolean"]:
         return NotImplementedError(f"Conversão para tipo '{type_name}' não é suportada.")

      return self.visitChildren(ctx)

   def visitTypeID(self, ctx:IIMLParser.TypeIDContext):
      return self.visitChildren(ctx)

