
import numpy as np
import cv2
from IIMLParserVisitor import IIMLParserVisitor

class IIMLInterpreter(IIMLParserVisitor):

    def __init__(self):
        self.env = {} 
        self.image = None

    def visitProgram(self, ctx):

        for stmt in ctx.stat():
            self.visit(stmt)

        return self.image.astype(np.uint8)

    def visitVarDeclStmt(self, ctx):

        var_name = ctx.ID().getText()
        value = self.visit(ctx.expr())
        self.env[var_name] = value

        return value

    def visitCreateImageStmt(self, ctx):
            
        width = int(self.visit(ctx.expr(0)))
        height = int(self.visit(ctx.expr(1)))
        background = float(self.visit(ctx.expr(2))) * 255
        self.image = np.full((height, width), background)

        return self.image

    def visitPlaceForm(self, ctx):

        draw_form = self.visit(ctx.createFormStmt())    

        x = int(self.visit(ctx.expr(0)))
        y = int(self.visit(ctx.expr(1)))
        intensity = float(self.visit(ctx.expr(2)))

        draw_form(x, y, intensity) 
        return self.image

    def visitCreateCircle(self, ctx):
        radius = int(self.visit(ctx.expr()))
        
        def _draw(x, y, intensity):
            cv2.circle(self.image, (x, y), radius, int(255 * intensity), thickness=-1)
        
        return _draw
    
    def visitCreateRect(self, ctx):
        
        width = int(self.visit(ctx.expr(0)))
        height = int(self.visit(ctx.expr(1)))
        
        def _draw(x, y, intensity):
            top_left = (x - width // 2, y - height // 2)
            bottom_right = (x + width // 2, y + height // 2)
            cv2.rectangle(self.image, top_left, bottom_right, int(255 * intensity), thickness=-1)
        
        return _draw
    
    def visitCreateCross(self, ctx):
        
        size = int(self.visit(ctx.expr(0)))
        thickness = int(self.visit(ctx.expr(1))) if len(ctx.expr()) == 2 else 1
        
        def _draw(x, y, intensity):
            color = int(255 * intensity)
            half = size // 2

            cv2.line(self.image, (x - half, y - half), (x + half, y + half), color, thickness)
            cv2.line(self.image, (x - half, y + half), (x + half, y - half), color, thickness)
        
        return _draw

    def visitCreatePlus(self, ctx):
        
        size = int(self.visit(ctx.expr(0)))
        thickness = int(self.visit(ctx.expr(1))) if len(ctx.expr()) == 2 else 1
        
        def _draw(x, y, intensity):
            color = int(255 * intensity)
            half = size // 2
            
            cv2.line(self.image, (x, y - half), (x, y + half), color, thickness)
            cv2.line(self.image, (x - half, y), (x + half, y), color, thickness)
        
        return _draw

    def visitCreateStar(self, ctx):
        
        radius = int(self.visit(ctx.expr(0)))
        thickness = int(self.visit(ctx.expr(1))) if len(ctx.expr()) == 2 else 1
        
        import math

        def _draw(x, y, intensity):
            color = int(255 * intensity)
            r = radius
            thickness = 2  
            
            points = []
            for i in range(5):
                angle_deg = 90 + i * 72          
                angle_rad = math.radians(angle_deg)
                px = int(x + r * math.cos(angle_rad))
                py = int(y - r * math.sin(angle_rad))
                points.append((px, py))
            
            star_order = [0, 2, 4, 1, 3, 0]
            for i in range(len(star_order) - 1):
                pt1 = points[star_order[i]]
                pt2 = points[star_order[i+1]]
                cv2.line(self.image, pt1, pt2, color, thickness)

        
        return _draw

    def visitPlaceLine(self, ctx):

        thickness = 1
        if len(ctx.expr()) == 1:
            
            thickness_val = self.visit(ctx.expr(0))
            thickness = int(thickness_val)



        def resolve_coord(line_ctx, index):
            var_ctx = line_ctx.expr(index)
            val = self.visit(var_ctx)

            return int(val)

        x1 = resolve_coord(ctx.lineExpr(0), 0)
        y1 = resolve_coord(ctx.lineExpr(0), 1)
        x2 = resolve_coord(ctx.lineExpr(1), 0)
        y2 = resolve_coord(ctx.lineExpr(1), 1)

        intensity = self.visit(ctx.expr()[-1]) * 255

        cv2.line(self.image, (x1,y1), (x2,y2), intensity, thickness)

        


    def visitReadStrExpr(self, ctx):
        prompt = ctx.STRING().getText().strip('"')
        value = input(f"{prompt} ")

        return float(value)

    def visitExprList(self, ctx):
        if ctx.first == None:
            return []
        list = [self.visit(l) for l in ctx.expr()]

        return list
    
    def visitNumberExpr(self, ctx):
        return float(ctx.getText())
    
    def visitStringExpr(self, ctx):
        return ctx.getText().strip('"')

    def visitIdExpr(self, ctx):
        name = ctx.getText()

        return self.env[name]

    def visitParenExpr(self, ctx):
        return self.visit(ctx.expr())

    def visitConvExpr(self, ctx):
 
        type_name = ctx.typeID().getText()
        value = self.visit(ctx.expr())

        if type_name == "number":
            return float(value)  

        elif type_name == "string":
            return str(value)

        elif type_name == "boolean":
            val = value
            if isinstance(val, str):
                return val.strip().lower() == "true"
            elif isinstance(val, (int, float)):
                return val != 0
            return bool(val)
        elif type_name == "list":
            if isinstance(value, list):
                return value
            else:
                return [value]

    def visitMulDivExpr(self, ctx):

        left = self.visit(ctx.e1)
        right = self.visit(ctx.e2)

        if ctx.op.type == ctx.MUL:
            return left * right
        else:
            return left / right

    def visitAddSubExpr(self, ctx):

        left = self.visit(ctx.e1)
        right = self.visit(ctx.e2)

        if ctx.op.type == ctx.FLIP:
            return left + right
        else:
            return left - right

    def visitForStmt(self, ctx):
        loop_var = ctx.ID().getText()
        collection_value = self.visit(ctx.expr())
        if not isinstance(collection_value, list):
            raise RuntimeError(
                f"Valor após 'for list {loop_var} within ...' não é lista, mas sim {type(collection_value).__name__}"
            )
        old_binding = self.env.get(loop_var, None)
        for element in collection_value:
            self.env[loop_var] = element
            for stmt_ctx in ctx.stat():
                self.visit(stmt_ctx)
        if old_binding is None:
            del self.env[loop_var]
        else:
            self.env[loop_var] = old_binding
        return None

    def visitIndexExpr(self, ctx):
        # ctx.e0 is the “list” expression, ctx.e1 is the index expression
        list_val = self.visit(ctx.e0)      # e.g. center  → should return a Python list
        idx      = self.visit(ctx.e1)      # e.g. “0”     → returns a number (float or int)

        # Make sure it really is a list and an integer index:
        if not isinstance(list_val, list):
            raise RuntimeError(f"Type error: trying to index a non‐list `{list_val}`")
        try:
            # Convert idx to an integer (round/truncate if necessary)
            i = int(idx)
        except Exception:
            raise RuntimeError(f"Index error: cannot convert `{idx}` to int")

        # Bounds check (optional but recommended):
        if i < 0 or i >= len(list_val):
            raise RuntimeError(f"Index out of range: tried to access {list_val}[{i}]")
        return list_val[i]
