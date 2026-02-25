parser grammar IIMLParser;

options {
    tokenVocab = IIMLLexer; 
}

program
    : ( stat | NEWLINE )* EOF
    ;



stat
    : varDeclStmt
    | createImageStmt
    | placeShapeStmt
    | forStmt
    ;

varDeclStmt
    : typeID ID ATR expr
    ;

createImageStmt
    : IMAGE SIZE expr BY expr BACKGROUND expr
    ;

placeShapeStmt
    : PLACE createFormStmt AT expr expr WITH INTENSITY expr                     #PlaceForm
    | PLACE LINE lineExpr lineExpr (WITH THICKNESS expr)? WITH INTENSITY expr   #PlaceLine
    ;

forStmt
    : FOR LISTT ID WITHIN expr NEWLINE+ stat+
    ;

lineExpr
    : LPAREN expr COMMA expr RPAREN
    ;

createFormStmt
    : CIRCLE RADIUS expr                           #CreateCircle
    | RECTANGLE WIDTH expr HEIGHT expr                   #CreateRect
    | CROSS expr (WITH THICKNESS expr)?     #CreateCross
    | PLUS expr (WITH THICKNESS expr)?      #CreatePlus
    | STAR expr (WITH THICKNESS expr)?      #CreateStar
    ; 

expr
    : e0=expr LBRACK e1=expr RBRACK          # IndexExpr
    | LBRACK (first=expr (COMMA expr)*)? RBRACK  # ExprList
    | e1=expr op=(MUL | DIV) e2=expr        #MulDivExpr
    | e1=expr op=(FLIP | SUB) e2=expr       #AddSubExpr
    | LPAREN expr RPAREN                    #ParenExpr
    | typeID LPAREN expr RPAREN             #ConvExpr
    | READ STRING                           #ReadStrExpr
    | NUMBER                                #NumberExpr
    | STRING                                #StringExpr
    | ID                                    #IdExpr
    ;

typeID 
    : NUMBERT           
    | STRINGT           
    | BOOLEANT          
    | LISTT OF t=typeID 
;
