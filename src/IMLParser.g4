parser grammar IMLParser;

options {
    tokenVocab = IMLLexer; // join lexer
}

program
    : stat* EOF 
;

stat
    : instantiation     #InstantiationStat
    | assignment        #AssignmentStat
    | ifStatement       #IfStat
    | untilLoop         #UntilStat
    | forLoop           #ForStat
    | command           #CommandStat
    | save              #SaveStat
    | listOperation     #ListOpStat
;


instantiation
    : typeID assignment
;

assignment returns[String varName]
    : ID ATR expression
;

command
    : PRINT expression  #PrintCommand
    | DRAW expression   #DrawCommand
;

save
    : e1=expression STORE INTO e2=expression
;

ifStatement
    : IF expression THEN thenBlock=statBlock (ELSE elseBlock=statBlock)? DONE
;

statBlock
    : stat+
;


untilLoop       
    : UNTIL expression DO statBlock DONE
;

forLoop
    : FOR typeID ID WITHIN expression DO statBlock DONE
;

listOperation
    : e1=expression op=(APPEND | POP | REMOVE) e2=expression
;

expression returns [Type eType, String varName]
    : sign=(FLIP|HFLIP|VFLIP| PXMINUS) expression                           #ExprUnary
    | typeID LPAREN expression RPAREN                                       #ExprTypeConvertion
    | LPAREN expression RPAREN                                              #ExprParenthesis
    | NOT expression                                                        #ExprReverse
    | (COLUMNS | ROWS) OF expression                                        #ExprColumns
    | e1=expression op=(OPEN | CLOSE | DILATE | ERODE) BY e2=expression     #ExprMorf
    | LOAD FROM expression                                                  #ExprLoad
    | e1=expression op=(MUL | DIV) e2=expression                            #ExprMultDiv
    | e1=expression op=(FLIP | VFLIP) e2=expression                         #ExprAddSub
    | e1=expression op=(PXMUL | PXDIV) e2=expression                        #ExprPxMultDiv
    | e1=expression op=(PXPLUS | PXMINUS) e2=expression                     #ExprPxAddSub
    | e1=expression op=(GT | LT | GTE | LTE) e2=expression                  #ExprRelational
    | e1=expression op=(EQ | NEQ) e2=expression                             #ExprEqNeq
    | e1=expression op=(PXGT | PXLT | PXGTE | PXLTE) e2=expression          #ExprPxRelational
    | e1=expression op=(PXEQ | PXNEQ) e2=expression                         #ExprPxEqNeq
    | op=(ALL | ANY) PIXEL expression                                       #ExprAllAny
    | COUNT PIXEL e1=expression IN e2=expression                            #ExprCount
    | e1=expression AND e2=expression                                       #ExprAnd
    | e1=expression OR e2=expression                                        #ExprOr
    | e1=expression op=(HSCALE | VSCALE | SCALE) e2=expression              #ExprScale
    | e1=expression op=(TOP | BLACK) HAT BY e2=expression                   #ExprHats
    | LBRACK (firstExpr=expression (COMMA expression)*)? RBRACK             #ExprList
    | READ expression                                                       #ExprRead
    | RUN FROM expression                                                   #ExprRun
    | expression (LBRACK expression RBRACK)+                                #ExprListID
    | PERCENTAGE                                                            #ExprPercentage
    | NUMBER                                                                #ExprNumber
    | BOOLEAN                                                               #ExprBoolean                   
    | STRING                                                                #ExprString                              
    | ID                                                                    #ExprID
;


typeID returns[Type res]
    : NUMBERT           {$res = new NumberType();}
    | STRINGT           {$res = new StringType();}
    | BOOLEANT          {$res = new BooleanType();}
    | PERCENTAGET       {$res = new PercentageType();}
    | IMAGE             {$res = new ImageType();}
    | LISTT OF t=typeID {$res = new ListType($t.res);}
;
